package Whatsapp.managingServer;

import Whatsapp.chatUser.ChatActor;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import javafx.util.Pair;

import java.io.Serializable;
import java.util.HashMap;

public class GroupActor extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private Pair<String, ActorRef> admin;
    private HashMap<String, ActorRef> coAdmins;
    private HashMap<String, ActorRef> users;
    private HashMap<String, ActorRef> mutedUsers;
    private String groupname;

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
            // TODO: I think print here won't reach anywhere
            log.error("Admin is already set for group");
            return;
        }

        this.admin = new Pair<String, ActorRef>(username, userPath);
        // TODO: I think print here won't reach anywhere
        log.info(String.format("%s is set to admin in group %s", username, groupname));
    }

    private void leaveGroup(GroupLeaveMessage leaveGroupMessage) {
        if (admin.getKey().equals(leaveGroupMessage.username)) {
            // TODO: send broadcast of UserLeftGroupMessage
            // TODO: Remove all users and broadcast and send group closed as broadcast
            getContext().parent().tell(new ManagingActor.GroupDeleteMessage(groupname), getSelf());
            // TODO: Stop group (maybe in server)
        }
        // TODO: send broadcast of UserLeftGroupMessage
        coAdmins.remove(leaveGroupMessage.username);
        // TODO: Send UserLeftGroupMessage
        users.remove(leaveGroupMessage.username);
        if (mutedUsers.containsKey(leaveGroupMessage.username)) {

        } else {
            getSender().tell(new ChatActor.GroupLeaveError(String.format("%s is not in %s!",
                    leaveGroupMessage.username, leaveGroupMessage.groupname)), getSelf());
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
