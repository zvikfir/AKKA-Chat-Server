package Whatsapp.managingServer;

import Whatsapp.Messages.ChatActorMessages;
import Whatsapp.Messages.GroupActorMessages.*;
import Whatsapp.Messages.ManagingActorMessages;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class GroupActor extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    Router router;
    private String adminUsername;
    private List<String> coAdmins;
    private List<String> users;
    private List<String> mutedUsers;
    private String groupName;

    {
        router = new Router(new BroadcastRoutingLogic());
    }

    public GroupActor(String groupName) {
        this.groupName = groupName;
        coAdmins = new ArrayList<String>();
        users = new ArrayList<String>();
        mutedUsers = new ArrayList<String>();
    }

    static public Props props(String groupName) {
        return Props.create(GroupActor.class, groupName);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GroupCreateMessage.class, this::createGroup)
                .match(GroupAddCoAdmin.class, this::addCoAdmin)
                .match(GroupRemoveCoAdmin.class, this::removeCoAdmin)
                .match(ChatActorMessages.UserChatGroupTextMessage.class, msg -> sendMsgToGroup(msg.username, msg))
                .match(ChatActorMessages.UserChatGroupFileMessage.class, msg -> sendMsgToGroup(msg.username, msg))
                .match(ValidateGroupInviteMessage.class, this::validateInviteUser)
                .match(ChatActorMessages.GroupInvitationAccepted.class, msg -> addUser(msg.invited))
                .match(GroupLeaveMessage.class, msg -> {
                    if (!users.contains(msg.userName)) {
                        getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s is not in %s",
                                msg.userName, groupName)), getSelf());
                        return;
                    }
                    deleteUser(msg.userName, getSender());
                    router.route(new ChatActorMessages.ManagingMessage(String.format("%s has left %s!", msg.userName,
                            groupName)), getSelf());
                })
                .match(GroupRemoveUserMessage.class, this::removeUser)
                .match(GroupUserMute.class, this::muteUser)
                .match(GroupAutoUnmute.class, msg -> {
                    if (mutedUsers.contains(msg.username))
                        unmuteUser(msg.username, msg.targetRef);
                })
                .match(GroupUserUnmute.class, msg -> {
                    if (!checkCoAdminPrivileges(msg.username))
                        return;
                    unmuteUser(msg.targetUsername, msg.targetRef);
                })
                .match(ManagingActorMessages.UserDisconnectMessage.class, msg -> {
                    log.info(String.format("deleting user %s", msg.username));
                    deleteUser(msg.username, msg.userRef);
                })
                .build();
    }

    private void unmuteUser(String targetUsername, ActorRef targetRef) {
        if (mutedUsers.contains(targetUsername)) {
            mutedUsers.remove(targetUsername);
            targetRef.tell(new ChatActorMessages.ManagingMessage(String.format("You have been unmuted in %s! Muting " +
                    "time is up!", groupName)), getSelf());
        } else {
            targetRef.tell(new ChatActorMessages.ManagingMessage(String.format("%s is not muted!", targetUsername)),
                    getSelf());
        }
    }

    private void muteUser(GroupUserMute msg) {
        if (!checkCoAdminPrivileges(msg.username))
            return;

        if (!users.contains(msg.targetUsername)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s is not in %s",
                    msg.targetUsername, groupName)), getSelf());
            return;
        }

        if (mutedUsers.contains(msg.targetUsername)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s is already muted in %s",
                    msg.targetUsername, groupName)), getSelf());
            return;
        }

        mutedUsers.add(msg.targetUsername);

        getContext().getSystem().
                scheduler().scheduleOnce(Duration.ofSeconds(msg.timeInSeconds),
                getSelf(),
                new GroupAutoUnmute(msg.targetUsername, msg.targetActorRef),
                getContext().getSystem().dispatcher(),
                ActorRef.noSender());

        msg.targetActorRef.tell(new ChatActorMessages.ManagingMessage(String.format("You have been muted for %d in %s" +
                " by %s!", msg.timeInSeconds, groupName, msg.username)), getSelf());

    }

    private void sendMsgToGroup(String username, Object msg) {
        if (!users.contains(username)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("You are not part of %s!",
                    groupName)), getSelf());
            return;
        }
        if (mutedUsers.contains(username)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("You are muted in %s", groupName)),
                    getSelf());
            return;
        }
        router.route(msg, getSelf());
    }

    private boolean checkCoAdminPrivileges(String username) {
        if (!adminUsername.equals(username) && !coAdmins.contains(username)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("You are neither admin nor co-admin " +
                    "of %s!", groupName)), getSelf());
            return false;
        }
        return true;
    }

    private void validateInviteUser(ValidateGroupInviteMessage msg) {
        if (!checkCoAdminPrivileges(msg.username))
            return;

        if (users.contains(msg.targetUsername)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s is already in %s",
                    msg.targetUsername, groupName)), getSelf());
            return;
        }

        getSender().tell(msg, getSelf());
    }

    private void removeCoAdmin(GroupRemoveCoAdmin msg) {
        if (!users.contains(msg.targetUsername)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s does not exist!",
                    msg.targetUsername)), getSelf());
            return;
        }
        if (!checkCoAdminPrivileges(msg.username))
            return;

        coAdmins.remove(msg.targetUsername);

        msg.targetUserActorRef.tell(new ChatActorMessages.ManagingMessage(String.format("You have been demoted to " +
                "user in %s!", groupName)), getSelf());
    }

    private void addCoAdmin(GroupAddCoAdmin msg) {
        if (!users.contains(msg.targetUsername)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s does not exist!",
                    msg.targetUsername)), getSelf());
            return;
        }
        if (!checkCoAdminPrivileges(msg.username))
            return;

        coAdmins.add(msg.targetUsername);

        msg.targetUserActorRef.tell(new ChatActorMessages.ManagingMessage(String.format("You have been promoted to " +
                "co-admin in %s!", groupName)), getSelf());
    }

    private void createGroup(GroupCreateMessage msg) {
        if (adminUsername != null) {
            return;
        }

        this.groupName = msg.groupname;

        addUser(msg.username);
        adminUsername = msg.username;

        router.route(new ChatActorMessages.ManagingMessage(String.format("%s created successfully!", groupName)),
                getSelf());
    }

    private void addUser(String username) {
        users.add(username);
        router = router.addRoutee(new ActorRefRoutee(getSender()));
    }

    private void deleteUser(String userName, ActorRef actorRef) {
        router = router.removeRoutee(actorRef);

        if (adminUsername.equals(userName)) {
            router.route(new ChatActorMessages.ManagingMessage(String.format("%s admin has closed %s!", groupName,
                    groupName)), getSelf());
            getContext().parent().tell(new ManagingActorMessages.GroupDeleteMessage(groupName), getSelf());
            return;
        }

        coAdmins.remove(userName);
        users.remove(userName);
        mutedUsers.remove(userName);
    }

    private void removeUser(GroupRemoveUserMessage msg) {
        if (!checkCoAdminPrivileges(msg.sourceUserName))
            return;
        if (!users.contains(msg.targetUserName)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s is not in %s!",
                    msg.targetUserName, groupName)), getSelf());
            return;
        }

        deleteUser(msg.targetUserName, msg.targetActorRef);

        msg.targetActorRef.tell(new ChatActorMessages.ManagingMessage(String.format("You have been removed from %s by" +
                " %s!", groupName, msg.sourceUserName)), getSelf());
    }
}
