package Whatsapp.managingServer;

import Whatsapp.chatUser.ChatUser;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;


public class ManagingServer extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private ArrayList<String> userNames;

    public ManagingServer() {
        this.userNames = new ArrayList<String>();
    }

    static public Props props() {
        return Props.create(ManagingServer.class, () -> new ManagingServer());
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
                    if (userNames.contains(connect.userName)) {
                        getSender().tell(new ChatUser.UserConnectFailure(
                                String.format("%s is in use!", connect.userName)), getSelf());
                    } else {
                        log.info("received connect");
                        this.userNames.add(connect.userName);
                        getSender().tell(new ChatUser.UserConnectSuccess(
                                String.format("%s has connected successfully!", connect.userName)), getSelf());
                    }
                })
                .match(Disconnect.class, discoonect -> {
                    this.userNames.remove(discoonect.username);
                    getSender().tell(new ChatUser.UserDisconnectSuccess(
                            String.format("%s has been disconnected successfully!", discoonect.username)), getSelf());
                })
                .build();
    }

    public static class Connect implements Serializable {
        String userName;

        public Connect(String userName) {
            this.userName = userName;
        }
    }

    public static class FetchTargetUserRef {
        String target;
        public FetchTargetUserRef(String target) {
            this.target = target;
        }
    }

    public static class Disconnect {
        String username;
        public Disconnect(String username) {
            this.username = username;
        }
    }
}
