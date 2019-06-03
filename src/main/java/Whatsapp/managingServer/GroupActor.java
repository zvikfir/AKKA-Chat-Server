package Whatsapp.managingServer;

import Whatsapp.chatUser.ChatActor;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import javafx.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GroupActor extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    Router router;
    private Pair<String, ActorRef> admin;
    private HashMap<String, ActorRef> coAdmins;
    private HashMap<String, ActorRef> users;
    private HashMap<String, ActorRef> mutedUsers;
    private String groupname;

    {
        List<Routee> routees = new ArrayList<Routee>();
        router = new Router(new BroadcastRoutingLogic(), routees);
    }

    public GroupActor(String groupname) {
        this.groupname = groupname;
    }

    static public Props props(String groupname) {
        return Props.create(GroupActor.class, groupname);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SetAdminMessage.class, msg -> setAdmin(msg.username, msg.userPath))
                .match(GroupLeaveMessage.class, this::leaveGroup)
                .build();
    }

    private void setAdmin(String username, ActorRef userPath) {
        if (admin != null) {
            return;
        }

        this.admin = new Pair<String, ActorRef>(username, userPath);
        getContext().watch(userPath);
        router.addRoutee(userPath);
    }

    private void leaveGroup(GroupLeaveMessage leaveGroupMessage) {
        String username = leaveGroupMessage.username;
        if (admin.getKey().equals(username) || coAdmins.containsKey(username) ||
                users.containsKey(username) || mutedUsers.containsKey(username)) {
            router.route(new ChatActor.UserLeftGroupMessage(String.format("%s has left %s!",
                    username, groupname)), getSelf());
            // Deletes if the user exists in one of this.
            coAdmins.remove(username);
            users.remove(username);
            mutedUsers.remove(username);
            if (admin.getKey().equals(username)) {
                // TODO: Remove all users and broadcast. Is it really needed?
                router.route(new ChatActor.UserLeftGroupMessage(String.format("%s admin has closed %s!", groupname,
                        groupname)), getSelf());
                getContext().parent().tell(new ManagingActor.GroupDeleteMessage(groupname), getSelf());
            }
        } else {
            getSender().tell(new ChatActor.GroupLeaveError(String.format("%s is not in %s!", username,
                    leaveGroupMessage.groupname)), getSelf());
        }
    }

    public static class SetAdminMessage {
        String username;
        ActorRef userPath;

        public SetAdminMessage(String username, ActorRef userPath) {
            this.username = username;
            this.userPath = userPath;
        }
    }

    public static class GroupLeaveMessage implements Serializable {
        final String username;
        final String groupname;
        final ActorRef sourcePath;

        public GroupLeaveMessage(String username, String groupname, ActorRef sourcePath) {
            this.username = username;
            this.groupname = groupname;
            this.sourcePath = sourcePath;
        }
    }
}
