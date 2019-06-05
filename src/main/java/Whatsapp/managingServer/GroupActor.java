package Whatsapp.managingServer;

import Whatsapp.chatUser.ChatActor;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;

import java.io.Serializable;
import java.time.LocalDateTime;
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
                .match(UserChatGroupTextMessage.class, msg -> {
                    router.route(new UserChatGroupTextMessage(msg.username, msg.groupName, msg.msg), getSelf());
                })
//                .match(GroupLeaveMessage.class, msg -> leaveGroup(msg.username))
                .build();
    }

    private void createGroup(GroupCreateMessage msg) {
        if (adminUsername != null) {
            return;
        }

        this.groupName = msg.groupname;

        addUser(msg.username);
        adminUsername = msg.username;
        router = router.addRoutee(new ActorRefRoutee(getSender()));

        router.route(new ChatActor.ManagingMessage(String.format("%s created successfully!", groupName)), getSelf());
    }

    private void addUser(String username) {
        users.add(username);
    }

//    private void leaveGroup(String username) {
//        if (adminUsername.equals(username) || coAdmins.containsKey(username) ||
//                users.contains(username) || mutedUsers.containsKey(username)) {
//            getSender().tell(new ChatActor.GroupLeaveSuccess(), getSelf());
//
//            // Deletes if the user exists in one of this.
//            coAdmins.remove(username);
//            users.remove(username);
//            mutedUsers.remove(username);
//
//            router.route(new ChatActor.UserLeftGroupMessage(String.format("%s has left %s!",
//                    username, groupName)), getSelf());
//
//            if (admin.getKey().equals(username)) {
//                // TODO: Remove all users. Is it really needed?
//                router.route(new ChatActor.UserLeftGroupMessage(String.format("%s admin has closed %s!", groupName,
//                        groupName)), getSelf());
//                getContext().parent().tell(new ManagingActor.GroupDeleteMessage(groupName), getSelf());
//            }
//        } else {
//            getSender().tell(new ChatActor.GroupLeaveError(String.format("%s is not in %s!", username, groupName)),
//                    getSelf());
//        }
//    }

    public static class GroupLeaveMessage implements Serializable {
        final String username;
        final ActorRef sourcePath;

        public GroupLeaveMessage(String username, ActorRef sourcePath) {
            this.username = username;
            this.sourcePath = sourcePath;
        }
    }

    public static class GroupCreateMessage implements Serializable {
        final String username;
        final String groupname;

        public GroupCreateMessage(String username, String groupname) {
            this.username = username;
            this.groupname = groupname;
        }
    }

    public static class UserChatGroupTextMessage implements Serializable {

        public final String username;
        public final String groupName;
        public final String msg;

        public String getMessage() {
            LocalDateTime now = LocalDateTime.now();
            String time = String.format("%d:%d", now.getHour(), now.getMinute());
            return String.format("[%s][%s][%s]%s", time, groupName, username, msg);
        }

        public UserChatGroupTextMessage(String username, String groupName, String msg) {
            this.username = username;
            this.groupName = groupName;
            this.msg = msg;
        }
    }
}
