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

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Connect.class, connect -> {
                    if (userNames.contains(connect.userName)) {
                        getSender().tell("no", getSelf());
                    } else {
                        log.info("received connect");
                        getSender().tell("ok", getSelf());
                        this.userNames.add(connect.userName);
                    }
                })
                .build();
    }

    public static class Connect implements Serializable {
        String userName;

        public Connect(String userName) {
            this.userName = userName;
        }
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

}

