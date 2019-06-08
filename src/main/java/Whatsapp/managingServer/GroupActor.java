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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupActor extends AbstractActor {
    final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private Router router;
    private String adminUserName;
    private List<String> coAdmins;
    private List<String> users;
    private Map<String, Long> mutedUsers;
    private String groupName;

    {
        router = new Router(new BroadcastRoutingLogic());
    }

    public GroupActor(String groupName) {
        this.groupName = groupName;
        coAdmins = new ArrayList<>();
        users = new ArrayList<>();
        mutedUsers = new HashMap<>();
    }

    static public Props props(String groupName) {
        return Props.create(GroupActor.class, groupName);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CreateGroupMessage.class, this::createGroup)
                .match(AddCoAdminMessage.class, this::addCoAdmin)
                .match(RemoveCoAdminMessage.class, this::removeCoAdmin)
                .match(ChatActorMessages.GroupTextMessage.class, msg -> sendMsgToGroup(msg.userName, msg))
                .match(ChatActorMessages.GroupFileMessage.class, msg -> sendMsgToGroup(msg.userName, msg))
                .match(ValidateInviteMessage.class, this::validateInviteUser)
                .match(ChatActorMessages.JoinGroupAcceptMessage.class, msg -> addUser(msg.invited))
                .match(LeaveGroupMessage.class, msg -> {
                    if (!users.contains(msg.userName)) {
                        getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s is not in %s",
                                msg.userName, groupName)), getSelf());
                        return;
                    }
                    deleteUser(msg.userName, getSender());
                    router.route(new ChatActorMessages.ManagingMessage(String.format("%s has left %s!", msg.userName,
                            groupName)), getSelf());
                })
                .match(RemoveUserMessage.class, this::removeUser)
                .match(MuteUserMessage.class, this::muteUser)
                .match(AutoUnmuteMessage.class, msg -> {
                    if (!mutedUsers.containsKey(msg.userName))
                        return;
                    mutedUsers.remove(msg.userName);
                    msg.targetRef.tell(new ChatActorMessages.ManagingMessage(String.format("You have been unmuted in " +
                            "%s! Muting time is up!", groupName)), getSelf());
                })
                .match(UnmuteUserMessage.class, msg -> {
                    if (checkCoAdminPrivileges(msg.userName))
                        return;
                    if (!mutedUsers.containsKey(msg.targetUserName)) {
                        getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s is not muted!",
                                msg.targetUserName)), getSelf());
                        return;
                    }

                    mutedUsers.remove(msg.targetUserName);
                    msg.targetRef.tell(new ChatActorMessages.ManagingMessage(String.format("You have been unmuted" +
                            " in %s by %s!", groupName, msg.userName)), getSelf());
                })
                .match(ManagingActorMessages.UserDisconnectMessage.class, msg -> {
                    log.info(String.format("deleting user %s", msg.userName));
                    deleteUser(msg.userName, msg.userRef);
                })
                .build();
    }

    private void muteUser(MuteUserMessage msg) {
        if (checkCoAdminPrivileges(msg.userName))
            return;

        if (!users.contains(msg.targetUserName)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s is not in %s",
                    msg.targetUserName, groupName)), getSelf());
            return;
        }

        if (mutedUsers.containsKey(msg.targetUserName)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s is already muted in %s",
                    msg.targetUserName, groupName)), getSelf());
            return;
        }

        mutedUsers.put(msg.targetUserName, msg.timeInSeconds);

        getContext().getSystem().
                scheduler().scheduleOnce(Duration.ofSeconds(msg.timeInSeconds),
                getSelf(),
                new AutoUnmuteMessage(msg.targetUserName, msg.targetActorRef),
                getContext().getSystem().dispatcher(),
                ActorRef.noSender());

        msg.targetActorRef.tell(new ChatActorMessages.ManagingMessage(String.format("You have been muted for %d in %s" +
                " by %s!", msg.timeInSeconds, groupName, msg.userName)), getSelf());

    }

    private void sendMsgToGroup(String userName, Object msg) {
        if (!users.contains(userName)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("You are not part of %s!",
                    groupName)), getSelf());
            return;
        }
        if (mutedUsers.containsKey(userName)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("You are muted for %d seconds in " +
                    "%s!", mutedUsers.get(userName), groupName)), getSelf());
            return;
        }
        router.route(msg, getSelf());
    }

    private boolean checkCoAdminPrivileges(String userName) {
        if (!adminUserName.equals(userName) && !coAdmins.contains(userName)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("You are neither admin nor co-admin " +
                    "of %s!", groupName)), getSelf());
            return true;
        }
        return false;
    }

    private void validateInviteUser(ValidateInviteMessage msg) {
        if (checkCoAdminPrivileges(msg.userName))
            return;

        if (users.contains(msg.targetUserName)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s is already in %s!",
                    msg.targetUserName, groupName)), getSelf());
            return;
        }

        getSender().tell(msg, getSelf());
    }

    private void removeCoAdmin(RemoveCoAdminMessage msg) {
        if (!users.contains(msg.targetUserName)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s does not exist!",
                    msg.targetUserName)), getSelf());
            return;
        }
        if (checkCoAdminPrivileges(msg.userName))
            return;

        coAdmins.remove(msg.targetUserName);

        msg.targetUserActorRef.tell(new ChatActorMessages.ManagingMessage(String.format("You have been demoted to " +
                "user in %s!", groupName)), getSelf());
    }

    private void addCoAdmin(AddCoAdminMessage msg) {
        if (!users.contains(msg.targetUserName)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s does not exist!",
                    msg.targetUserName)), getSelf());
            return;
        }
        if (checkCoAdminPrivileges(msg.userName))
            return;

        coAdmins.add(msg.targetUserName);

        msg.targetUserActorRef.tell(new ChatActorMessages.ManagingMessage(String.format("You have been promoted to " +
                "co-admin in %s!", groupName)), getSelf());
    }

    private void createGroup(CreateGroupMessage msg) {
        if (adminUserName != null)
            return;

        this.groupName = msg.groupName;
        addUser(msg.userName);
        adminUserName = msg.userName;
        router.route(new ChatActorMessages.ManagingMessage(String.format("%s created successfully!", groupName)),
                getSelf());
    }

    private void addUser(String userName) {
        users.add(userName);
        router = router.addRoutee(new ActorRefRoutee(getSender()));
    }

    private void deleteUser(String userName, ActorRef actorRef) {
        router = router.removeRoutee(actorRef);

        if (adminUserName.equals(userName)) {
            router.route(new ChatActorMessages.ManagingMessage(String.format("%s admin has closed %s!", groupName,
                    groupName)), getSelf());
            getContext().parent().tell(new ManagingActorMessages.GroupDeleteMessage(groupName), getSelf());
            return;
        }

        coAdmins.remove(userName);
        users.remove(userName);
        mutedUsers.remove(userName);
    }

    private void removeUser(RemoveUserMessage msg) {
        if (checkCoAdminPrivileges(msg.sourceUserName))
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
