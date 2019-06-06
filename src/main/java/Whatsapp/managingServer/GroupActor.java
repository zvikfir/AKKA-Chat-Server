package Whatsapp.managingServer;

import Whatsapp.Messages.ChatActorMessages;
import Whatsapp.chatUser.ChatActor;
import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;

import java.util.ArrayList;
import java.util.List;
import Whatsapp.Messages.GroupActorMessages.*;

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
                .match(GroupInviteMessage.class, this::inviteUser)
                .match(GroupAddCoAdmin.class, this::addCoAdmin)
                .match(GroupRemoveCoAdmin.class, this::removeCoAdmin)
                .match(ChatActorMessages.UserChatGroupTextMessage.class, msg -> router.route(msg, getSelf()))
                .match(ChatActorMessages.UserChatGroupFileMessage.class, msg -> router.route(msg, getSelf()))
                .match(ValidateGroupInviteMessage.class, this::validateInviteUser)
//                .match(GroupLeaveMessage.class, msg -> leaveGroup(msg.username))
                .build();
    }

    private boolean checkCoAdminPrivileges(String username) {
        if (!adminUsername.equals(username) && !coAdmins.contains(username)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("You are neither admin nor co-admin of %s!", groupName)), getSelf());
            return false;
        }
        return true;
    }

    private boolean checkUsernameExists(String username) {
        if (!users.contains(username)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s does not exist!", username)), getSelf());
            return false;
        }
        return true;
    }

    private void validateInviteUser(ValidateGroupInviteMessage msg) {
        if (!checkCoAdminPrivileges(msg.username))
            return;

        if (users.contains(msg.targetUsername)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s is already in %s", msg.targetUsername, groupName)), getSelf());
            return;
        }

        getSender().tell(msg, getSelf());
    }

    private void inviteUser(GroupInviteMessage msg) {
        addUser(msg.targetUsername);

        msg.targetUserActorRef.tell(new ChatActorMessages.ManagingMessage(String.format("Welcome to %s!", groupName)), getSelf());
    }

    private void removeCoAdmin(GroupRemoveCoAdmin msg) {
        if (!checkUsernameExists(msg.targetUsername) || !checkCoAdminPrivileges(msg.username))
            return;

        coAdmins.remove(msg.targetUsername);

        msg.targetUserActorRef.tell(new ChatActorMessages.ManagingMessage(String.format("You have been demoted to user in %s!", groupName)), getSelf());
    }

    private void addCoAdmin(GroupAddCoAdmin msg) {
        if (!checkUsernameExists(msg.targetUsername) || !checkCoAdminPrivileges(msg.username))
            return;

        coAdmins.add(msg.targetUsername);

        msg.targetUserActorRef.tell(new ChatActorMessages.ManagingMessage(String.format("You have been promoted to co-admin in %s!", groupName)), getSelf());
    }

    private void createGroup(GroupCreateMessage msg) {
        if (adminUsername != null) {
            return;
        }

        this.groupName = msg.groupname;

        addUser(msg.username);
        adminUsername = msg.username;

        router.route(new ChatActorMessages.ManagingMessage(String.format("%s created successfully!", groupName)), getSelf());
    }

    private void addUser(String username) {
        users.add(username);
        router = router.addRoutee(new ActorRefRoutee(getSender()));
    }

//    private void leaveGroup(String username) {
//        if (adminUsername.equals(username) || coAdmins.containsKey(username) ||
//                users.contains(username) || mutedUsers.containsKey(username)) {
//            getSender().tell(new ChatActorMessages.GroupLeaveSuccess(), getSelf());
//
//            // Deletes if the user exists in one of this.
//            coAdmins.remove(username);
//            users.remove(username);
//            mutedUsers.remove(username);
//
//            router.route(new ChatActorMessages.UserLeftGroupMessage(String.format("%s has left %s!",
//                    username, groupName)), getSelf());
//
//            if (admin.getKey().equals(username)) {
//                // TODO: Remove all users. Is it really needed?
//                router.route(new ChatActorMessages.UserLeftGroupMessage(String.format("%s admin has closed %s!", groupName,
//                        groupName)), getSelf());
//                getContext().parent().tell(new ManagingActor.GroupDeleteMessage(groupName), getSelf());
//            }
//        } else {
//            getSender().tell(new ChatActorMessages.GroupLeaveError(String.format("%s is not in %s!", username, groupName)),
//                    getSelf());
//        }
//    }


}
