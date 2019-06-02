package Whatsapp.managingServer;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import javafx.util.Pair;

import java.util.HashMap;

public class GroupActor extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private Pair<String, ActorRef> admin;
    private HashMap<String, ActorRef> coAdmins;
    private HashMap<String, ActorRef> users;
    private HashMap<String, ActorRef> mutedUsers;
    private String groupName;

    public GroupActor(String groupName) {
        this.groupName = groupName;
    }

    static public Props props(String groupName) { return Props.create(GroupActor.class, groupName); }


    public static class SetAdminMessage {
        String username;
        ActorRef userPath;

        public SetAdminMessage(String username, ActorRef userPath) {
            this.username = username;
            this.userPath = userPath;
        }
    }

    private void setAdmin(String username, ActorRef userPath) {
        if(admin != null) {
            log.error("Admin is already set for group");
            return;
        }

        this.admin = new Pair<String, ActorRef>(username, userPath);
        log.info(String.format("%s is set to admin in group %s", username, groupName));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SetAdminMessage.class, msg -> setAdmin(msg.username, msg.userPath))
                .build();
    }
}
