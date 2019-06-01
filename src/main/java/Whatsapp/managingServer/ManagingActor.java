package Whatsapp.managingServer;

import Whatsapp.chatUser.ChatActor;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;


public class ManagingActor extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private HashMap<String, ActorRef> users;
    private HashMap<String, ActorRef> groups;

    public ManagingActor() {
        this.users = new HashMap<String, ActorRef>();
    }

    static public Props props() {
        return Props.create(ManagingActor.class, ManagingActor::new);
    }

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("Whatsapp", ConfigFactory.load("managingServer"));

        try {
            final ActorRef managingServerActor = system.actorOf(ManagingActor.props(), "managingServer");

            System.out.println(">>> Press ENTER to exit <<<");
            System.in.read();

        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        } finally {
            system.terminate();
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Connect.class, this::connectRequest)
                .match(FetchTargetUserRef.class, this::fetchTargetRequest)
                .match(Disconnect.class, this::disconnectRequest)
                .match(GroupCrete.class, this::groupCreateRequest)
                .build();
    }

    private void groupCreateRequest(GroupCrete groupCreate) {
        if (groups.containsKey(groupCreate.groupname)) {
            getSender().tell(new ChatActor.GroupCreateFailure(
                    String.format("%s already exists!", groupCreate.groupname)), getSelf());
        } else {
            log.info("received group crete");
            ActorRef groupActor = getContext().actorOf(GroupActor.props(), "managingServer");
            // TODO: tell group actor to set the sender as group admin
            this.groups.put(groupCreate.groupname, groupActor);
            getSender().tell(new ChatActor.UserConnectSuccess(
                    String.format("%s created successfully!", groupCreate.groupname)), getSelf());
        }
    }

    private void connectRequest(Connect connect) {
        if (users.containsKey(connect.username)) {
            getSender().tell(new ChatActor.UserConnectFailure(
                    String.format("%s is in use!", connect.username)), getSelf());
        } else {
            log.info("received connect");
            this.users.put(connect.username, connect.sourcePath);
            getSender().tell(new ChatActor.UserConnectSuccess(
                    String.format("%s has connected successfully!", connect.username)), getSelf());
        }
    }

    private void disconnectRequest(Disconnect disconnect) {
        this.users.remove(disconnect.username);
        getSender().tell(new ChatActor.UserDisconnectSuccess(
                String.format("%s has been disconnected successfully!", disconnect.username)), getSelf());
    }

    private void fetchTargetRequest(FetchTargetUserRef fetchTarget) {
        ActorRef target = null;
        if (users.containsKey(fetchTarget.target)) {
            target = users.get(fetchTarget.target);
        }
        getSender().tell(target, getSelf());
    }

    public static class Connect implements Serializable {
        final String username;
        final ActorRef sourcePath;

        public Connect(String username, ActorRef sourcePath) {
            this.username = username;
            this.sourcePath = sourcePath;
        }
    }

    public static class FetchTargetUserRef implements Serializable {
        String target;

        public FetchTargetUserRef(String target) {
            this.target = target;
        }
    }

    public static class Disconnect implements Serializable {
        String username;

        public Disconnect(String username) {
            this.username = username;
        }
    }

    public static class GroupCrete implements Serializable {
        final String groupname;
        final ActorRef sourcePath;


        public GroupCrete(String groupname, ActorRef sourcePath) {
            this.groupname = groupname;
            this.sourcePath = sourcePath;
        }
    }
}
