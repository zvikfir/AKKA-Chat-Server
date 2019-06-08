package Whatsapp.managingServer;

import Whatsapp.Messages.ChatActorMessages;
import Whatsapp.Messages.GroupActorMessages.*;
import Whatsapp.Messages.ManagingActorMessages;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupActor extends AbstractActor {
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
                .match(CreateGroupMessage.class, msg -> {
                    // if admin is not null then this group already has an admin and this is an error that shouldn't
                    // happen
                    if (adminUserName != null)
                        return;

                    this.groupName = msg.groupName;
                    addUser(msg.userName);
                    adminUserName = msg.userName;
                    router.route(new ChatActorMessages.ManagingMessage(String.format("%s created successfully!",
                            groupName)), getSelf());
                })
                .match(AddCoAdminMessage.class, msg -> {
                    if (!users.contains(msg.targetUserName)) {
                        getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s does not exist!",
                                msg.targetUserName)), getSelf());
                        return;
                    }
                    if (checkCoAdminPrivileges(msg.userName))
                        return;

                    coAdmins.add(msg.targetUserName);

                    msg.targetUserActorRef.tell(new ChatActorMessages.ManagingMessage(String.format("You have been " +
                            "promoted to co-admin in %s!", groupName)), getSelf());
                })
                .match(RemoveCoAdminMessage.class, msg -> {
                    if (!users.contains(msg.targetUserName)) {
                        getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s does not exist!",
                                msg.targetUserName)), getSelf());
                        return;
                    }
                    if (checkCoAdminPrivileges(msg.userName))
                        return;

                    coAdmins.remove(msg.targetUserName);

                    msg.targetUserActorRef.tell(new ChatActorMessages.ManagingMessage(String.format("You have been " +
                            "demoted to user in %s!", groupName)), getSelf());
                })
                .match(ChatActorMessages.GroupTextMessage.class, msg -> sendMsgToGroup(msg.userName, msg))
                .match(ChatActorMessages.GroupFileMessage.class, msg -> sendMsgToGroup(msg.userName, msg))
                .match(ValidateInviteMessage.class, msg -> {
                    if (checkCoAdminPrivileges(msg.userName))
                        return;

                    if (users.contains(msg.targetUserName)) {
                        getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s is already in %s!",
                                msg.targetUserName, groupName)), getSelf());
                        return;
                    }
                    // This message is sent back to the user as confirmation that he can invite another user to this
                    // group
                    getSender().tell(msg, getSelf());
                })
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
                .match(RemoveUserMessage.class, msg -> {
                    if (checkCoAdminPrivileges(msg.sourceUserName))
                        return;
                    if (!users.contains(msg.targetUserName)) {
                        getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s is not in %s!",
                                msg.targetUserName, groupName)), getSelf());
                        return;
                    }

                    deleteUser(msg.targetUserName, msg.targetActorRef);

                    msg.targetActorRef.tell(new ChatActorMessages.ManagingMessage(String.format("You have been " +
                            "removed from %s by %s!", groupName, msg.sourceUserName)), getSelf());
                })
                .match(MuteUserMessage.class, msg -> {
                    if (checkCoAdminPrivileges(msg.userName))
                        return;

                    if (!users.contains(msg.targetUserName)) {
                        getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s is not in %s",
                                msg.targetUserName, groupName)), getSelf());
                        return;
                    }

                    // Check if the user is already muted
                    if (mutedUsers.containsKey(msg.targetUserName)) {
                        getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s is already muted in " +
                                "%s", msg.targetUserName, groupName)), getSelf());
                        return;
                    }

                    mutedUsers.put(msg.targetUserName, msg.timeInSeconds);

                    getContext().getSystem().scheduler().scheduleOnce(Duration.ofSeconds(msg.timeInSeconds),
                            getSelf(), new AutoUnmuteMessage(msg.targetUserName, msg.targetActorRef),
                            getContext().getSystem().dispatcher(), ActorRef.noSender());

                    msg.targetActorRef.tell(new ChatActorMessages.ManagingMessage(String.format("You have been muted " +
                            "for %d in %s by %s!", msg.timeInSeconds, groupName, msg.userName)), getSelf());
                })
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
                    msg.targetRef.tell(new ChatActorMessages.ManagingMessage(String.format("You have been unmuted in " +
                            "%s by %s!", groupName, msg.userName)), getSelf());
                })
                .match(ManagingActorMessages.UserDisconnectMessage.class, msg -> {
                    deleteUser(msg.userName, msg.userRef);
                    router.route(new ChatActorMessages.ManagingMessage(String.format("%s has left %s!", msg.userName,
                            groupName)), getSelf());
                })
                .build();
    }

    /**
     * Sends either a text message or a FileMessage to the entire group
     * The function includes validation that the sender is a member of the group, and that it isn't included in the
     * muted users list
     *
     * @param userName the name of message sender
     * @param msg      The message object
     */
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

    /**
     * Checks for admin/co-admin privileges, or in other words, if userName is either an admin or in the co-admins list
     *
     * @param userName The name of the user requesting to perform a certain operation
     * @return whether the user has privileges or not
     */
    private boolean checkCoAdminPrivileges(String userName) {
        if (!adminUserName.equals(userName) && !coAdmins.contains(userName)) {
            // Assumes that the message's sender is the user requesting to perform the operation
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("You are neither admin nor co-admin " +
                    "of %s!", groupName)), getSelf());
            return true;
        }
        return false;
    }

    private void addUser(String userName) {
        users.add(userName);
        router = router.addRoutee(new ActorRefRoutee(getSender()));
    }

    private void deleteUser(String userName, ActorRef actorRef) {
        router = router.removeRoutee(actorRef);

        // If the user to be deleted is the admin of the group, the group should be deleted at once
        if (adminUserName.equals(userName)) {
            router.route(new ChatActorMessages.ManagingMessage(String.format("%s admin has closed %s!", groupName,
                    groupName)), getSelf());
            // Sends the ManagingActor a notification of this group deletion so it can delete any record of it on its
            // local storage
            getContext().parent().tell(new ManagingActorMessages.GroupDeleteMessage(groupName), getSelf());
            return;
        }

        coAdmins.remove(userName);
        users.remove(userName);
        mutedUsers.remove(userName);
    }
}
