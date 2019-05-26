package Whatsapp.chatUser;

import Whatsapp.managingServer.ManagingServer;
import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ChatUser extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    static public Props props() {
        return Props.create(ChatUser.class, () -> new ChatUser());
    }

    public static class UserConnectSuccess implements Serializable {
        String msg;

        public UserConnectSuccess(String msg) {
            this.msg = msg;
        }
    }

    public static class UserConnectFailure implements Serializable {
        String msg;

        public UserConnectFailure(String msg) {
            this.msg = msg;
        }
    }

    public static class UserDisconnectSuccess {
        String msg;

        public UserDisconnectSuccess(String msg) {
            this.msg = msg;
        }
    }

    public static class UserChatTextMessage {
        String source;
        String message;

        public String getMessage() {
            LocalDateTime now = LocalDateTime.now();
            String time = String.format("%d:%d", now.getHour(), now.getMinute());
            return String.format("[%s][user][%s]%s", time, source, message);
        }

        public UserChatTextMessage(String source, String message) {
            this.source = source;
            this.message = message;
        }
    }

    public static class UserChatFileMessage {
        String source;
        byte[] file;
        String targetFilePath;

        public UserChatFileMessage(String source, byte[] file) {
            this.source = source;
            this.file = file;
        }
    }

    //    final ActorSelection managingServer = getContext().actorSelection("akka://Whatsapp@192.168.0.96:2552/user/managingServer");
    final static Timeout timeout = new Timeout(Duration.create(1, TimeUnit.SECONDS));

    public ChatUser() {
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(UserChatTextMessage.class, msg -> {
                    System.out.println(msg.getMessage());
                })
                .build();
    }

    private static void connect(ActorSelection managingServer, ActorRef user, String username) {
        Future<Object> rt = Patterns.ask(managingServer, new ManagingServer.Connect(username), timeout);
        try {
            Object result = Await.result(rt, timeout.duration());
            if (result instanceof UserConnectSuccess) {
                System.out.println(((UserConnectSuccess) result).msg);
            } else {
                System.out.println(((UserConnectFailure) result).msg);
            }
        } catch (Exception e) {
            System.out.println("server is offline!");
        }
    }

//    private void disconnect() {
//        Future<Object> rt = Patterns.ask(managingServer, new ManagingServer.Disconnect(username), timeout);
//        try {
//            Object result = Await.result(rt, timeout.duration());
//            if (result instanceof UserDisconnectSuccess) {
//                System.out.println(((UserDisconnectSuccess) result).msg);
//            }
//        } catch (Exception e) {
//            System.out.println("server is offline!");
//        }
//    }
//
//    private void text(String target, String msg) {
//        ActorRef targetRef = fetchTargetRef(target);
//
//        if(targetRef == ActorRef.noSender())
//            return;
//
//        targetRef.tell(new UserChatTextMessage(username, msg), getSelf());
//    }
//
//
//    private void file(String target, byte[] file) {
//        ActorRef targetRef = fetchTargetRef(target);
//
//        if(targetRef == ActorRef.noSender())
//            return;
//
//        targetRef.tell(new UserChatFileMessage(username, file), getSelf());
//    }
//
//    private ActorRef fetchTargetRef(String target) {
//        ActorRef targetRef = null;
//        if(contacts.containsKey(target)) {
//            targetRef = contacts.get(target);
//        }
//        else {
//            Future<Object> rt = Patterns.ask(managingServer, new ManagingServer.FetchTargetUserRef(target), timeout);
//            try {
//                targetRef = (ActorRef) Await.result(rt, timeout.duration());
//            } catch (Exception e) {
//                System.out.println("server is offline!");
//            }
//        }
//
//        if(targetRef == ActorRef.noSender())
//            System.out.println(String.format("%s does not exist!", target));
//
//        return targetRef;
//    }

    private static void cli(ActorSelection managingServer, ActorRef user) {
        Scanner in = new Scanner(System.in);
        do {
            System.out.print(">>");
            String input = in.nextLine();

            if(input.startsWith("/user")) {
                String[] cmd_parts = input.split("\\s+");
                cli_user(managingServer, user, cmd_parts);
            }


        } while(true);
    }

    private static void cli_user(ActorSelection managingServer, ActorRef user, String[] cmd_parts) {
        String cmd = cmd_parts[1];
        switch(cmd) {
            case "connect":
                connect(managingServer, user, cmd_parts[2]);
                break;
            case "disconnect":
                break;
            case "text":
                break;
            case "file":
                break;
        }
    }

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("chatUser", ConfigFactory.load("chatUser"));
        final ActorSelection managingServer = system.actorSelection("akka://Whatsapp@127.0.0.1:2552/user/managingServer");
        try {
            final ActorRef user =
                    system.actorOf(ChatUser.props(), "user");

            cli(managingServer, user);
        } catch (Exception ioe) {
        } finally {
            system.terminate();
        }
    }
}
