package Whatsapp.managingServer;

import Whatsapp.chatUser.ChatUser;
import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;


public class ManagingServer extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private HashMap<String, ActorRef> users;

    public ManagingServer() {
        this.users = new HashMap<String, ActorRef>();
    }

    static public Props props() {
        return Props.create(ManagingServer.class, ManagingServer::new);
    }

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("Whatsapp", ConfigFactory.load("managingServer"));

        try {
            //#create-actors
            final ActorRef managingServerActor =
                    system.actorOf(ManagingServer.props(), "managingServer");
            //#create-actors

            System.out.println(">>> Press ENTER to exit <<<");
            System.in.read();

        } catch (IOException ioe) {
        } finally {
            system.terminate();
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Connect.class, connect -> {
                    if (users.containsKey(connect.userName)) {
                        getSender().tell(new ChatUser.UserConnectFailure(
                                String.format("%s is in use!", connect.userName)), getSelf());
                    } else {
                        log.info("received connect");
                        this.users.put(connect.userName, connect.sourcePath);
                        getSender().tell(new ChatUser.UserConnectSuccess(
                                String.format("%s has connected successfully!", connect.userName)), getSelf());
                    }
                })
                .match(Disconnect.class, disconnect -> {
                    this.users.remove(disconnect.username);
                    getSender().tell(new ChatUser.UserDisconnectSuccess(
                            String.format("%s has been disconnected successfully!", disconnect.username)), getSelf());
                })
                .match(FetchTargetUserRef.class, fetchTarget -> {
                    ActorRef target = null;
                    if (users.containsKey(fetchTarget.target)) {
                        target = users.get(fetchTarget.target);
                    }
                    getSender().tell(target, getSelf());
                })
                .build();
    }

    public static class Connect implements Serializable {
        final String userName;
        final ActorRef sourcePath;

        public Connect(String userName, ActorRef sourcePath) {
            this.userName = userName;
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
}
