package Whatsapp.chatUser;

import Whatsapp.managingServer.ManagingServer;
import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.remote.SystemMessageFormats;
import akka.util.Timeout;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ChatUser extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    static public Props props(String username) {
        return Props.create(ChatUser.class, () -> new ChatUser(username));
    }

    public static class UserConnectSuccess {
        String msg;

        public UserConnectSuccess(String msg) {
            this.msg = msg;
        }
    }

    public static class UserConnectFailure {
        String msg;

        public UserConnectFailure(String msg) {
            this.msg = msg;
        }
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
    final ActorSelection managingServer = getContext().actorSelection("akka://Whatsapp@127.0.0.1:2552/user/managingServer");
//    final ActorSelection managingServer = getContext().actorSelection("akka://Whatsapp@192.168.0.96:2552/user/managingServer");
    final Timeout timeout = new Timeout(Duration.create(1, TimeUnit.SECONDS));

    public ChatUser(String username) {
        this.username = username;
    }

    @Override
    public void preStart() throws IOException {

        do {
            Scanner in = new Scanner(System.in);
            System.out.print("Enter username:");
            String username = in.nextLine();

            Future<Object> rt = Patterns.ask(managingServer, new ManagingServer.Connect(username), timeout);
            try {
                Object result = Await.result(rt, timeout.duration());
                if (result instanceof UserConnectSuccess) {
                    this.username = username;
                    System.out.println(((UserConnectSuccess) result).msg);
                    return;
                }
                else {
                    System.out.println(((UserConnectFailure) result).msg);
                }
            } catch (Exception e) {
                System.out.println("server is offline!");
            }
        } while(true);
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
            final ActorRef user =
                    system.actorOf(ChatUser.props("Yossi"), "user");
        } catch (Exception ioe) {
        } finally {
            system.terminate();
        }
    }
}
