package Whatsapp.chatUser;

import Whatsapp.managingServer.ManagingServer;
import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Scanner;

public class ChatUser extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    static public Props props(String username) {
        return Props.create(ChatUser.class, () -> new ChatUser(username));
    }

    public static class ChatMessage {
        String groupName;
        String sourceUserName;
        String message;

        public String getMessage() {
            LocalDateTime now = LocalDateTime.now();
            String time = String.format("%d:%d", now.getHour(), now.getMinute());
            return String.format("[%s][%s][%s]%s", time, groupName, sourceUserName, message);
        }

        public ChatMessage(String groupName, String sourceUserName, String message) {
            this.groupName = groupName;
            this.sourceUserName = sourceUserName;
            this.message = message;
        }
    }

    String username;

    public ChatUser(String username) {
        this.username = username;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, msg -> {
                    log.info(msg);
                })
                .match(ChatMessage.class, msg -> {
                    System.out.println(msg.getMessage());
                })
                .build();
    }

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("chatUser", ConfigFactory.load("chatUser"));

        try {
            Scanner in = new Scanner(System.in);
            System.out.print("Enter username:");
            String username = in.nextLine();
            System.out.print("Username is - " + username);

            ActorSelection managingServer = system.actorSelection("akka://Whatsapp@127.0.0.1:2552/user/managingServer");


            final ActorRef user =
                    system.actorOf(ChatUser.props("Yossi"), "user");

            

            managingServer.tell(new ManagingServer.Connect(username), user);

            System.out.println(">>> Press ENTER to exit <<<");
            System.in.read();

        } catch (IOException ioe) {
        } finally {
            system.terminate();
        }

    }
}
