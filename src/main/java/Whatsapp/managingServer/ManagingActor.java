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
        this.groups = new HashMap<String, ActorRef>();
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
                .match(UserConnectMessage.class, this::connectRequest)
                .match(FetchTargetUserRef.class, this::fetchTargetRequest)
                .match(UserDisconnectMessage.class, this::disconnectRequest)
                .match(GroupCreateMessage.class, this::groupCreateRequest)
                .build();
    }

    private void groupCreateRequest(GroupCreateMessage groupCreate) {
        if (groups.containsKey(groupCreate.groupName)) {
            getSender().tell(new ChatActor.GroupCreateFailure(
                    String.format("%s already exists!", groupCreate.groupName)), getSelf());
        } else {
            log.info("received group create");
            ActorRef groupActor = getContext().actorOf(GroupActor.props(groupCreate.groupName), groupCreate.groupName);
            // TODO: tell group actor to set the sender as group admin
            this.groups.put(groupCreate.groupName, groupActor);

            groupActor.tell(new GroupActor.SetAdminMessage(groupCreate.username, groupCreate.sourcePath), getSelf());

            getSender().tell(new ChatActor.GroupCreateSuccess(
                    String.format("%s created successfully!", groupCreate.groupName)), getSelf());
        }
    }

    private void connectRequest(UserConnectMessage connect) {
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

    private void disconnectRequest(UserDisconnectMessage disconnect) {
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

    public static class UserConnectMessage implements Serializable {
        final String username;
        final ActorRef sourcePath;

        public UserConnectMessage(String username, ActorRef sourcePath) {
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

    public static class UserDisconnectMessage implements Serializable {
        String username;

        public UserDisconnectMessage(String username) {
            this.username = username;
        }
    }

    public static class GroupCreateMessage implements Serializable {
        final String username;
        final String groupName;
        final ActorRef sourcePath;

        public GroupCreateMessage(String username, String groupName, ActorRef sourcePath) {
            this.username = username;
            this.groupName = groupName;
            this.sourcePath = sourcePath;
        }
    }
}
