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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ChatUser extends AbstractActor {
    final static Timeout timeout = new Timeout(Duration.create(1, TimeUnit.SECONDS));
    final ActorSelection managingServer = getContext().actorSelection("akka://Whatsapp@127.0.0.1:2552/user/managingServer");
    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    String username;

    static public Props props() {
        return Props.create(ChatUser.class, () -> new ChatUser());
    }

    private static void cli(ActorRef user) {
        Scanner in = new Scanner(System.in);
        do {
            System.out.print(">>");
            String input = in.nextLine();

            if (input.startsWith("/user")) {
                String[] cmd_parts = input.split("\\s+");
                cli_user(user, cmd_parts);
            }


        } while (true);
    }

    private static void cli_user(ActorRef user, String[] cmd_parts) {
        String cmd = cmd_parts[1];
        switch (cmd) {
            // /user connect <username>
            case "connect":
                user.tell(new ConnectControlMessage(cmd_parts[2]), ActorRef.noSender());
                break;
            // /user disconnect
            case "disconnect":
                user.tell(new DisconnectControlMessage(), ActorRef.noSender());
                break;
            // /user text <target> <message>
            case "text":
                user.tell(new TextControlMessage(cmd_parts[2], cmd_parts[3]), ActorRef.noSender());
                break;
            // /user file <target> <sourceFilePath>
            case "file":
                cli_user_file(user, cmd_parts[2], cmd_parts[3]);
                break;
        }
    }

    public static void cli_user_file(ActorRef user, String target, String filePath) {
        byte[] fileContent;
        try {
            fileContent = Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            System.out.println(String.format("%s does not exist!", filePath));
            return;
        }

        user.tell(new FileControlMessage(target, fileContent), ActorRef.noSender());
    }

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("chatUser", ConfigFactory.load("chatUser"));
        try {
            final ActorRef user =
                    system.actorOf(ChatUser.props(), "user");
            cli(user);
        } catch (Exception ioe) {
            System.out.println(ioe.getMessage());
        } finally {
            system.terminate();
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ConnectControlMessage.class, msg -> connect(msg.username))
                .match(DisconnectControlMessage.class, msg -> disconnect())
                .match(TextControlMessage.class, msg -> text(msg.target, msg.msg))
                .match(FileControlMessage.class, msg -> file(msg.target, msg.file))
                .match(UserChatTextMessage.class, msg -> log.info(msg.getMessage()))
                .match(UserChatFileMessage.class, msg -> {
                    File tmpFile = File.createTempFile("chatUser-file", ".tmp");
                    String targetFilePath = tmpFile.getAbsolutePath();
                    FileOutputStream out = new FileOutputStream(targetFilePath);
                    out.write(msg.file);
                    out.close();

                    log.info(msg.getMessage(targetFilePath));
                })
                .build();
    }

    private void connect(String username) {
        Future<Object> rt = Patterns.ask(managingServer, new ManagingServer.Connect(username, getSelf()), timeout);
        try {
            Object result = Await.result(rt, timeout.duration());
            if (result instanceof UserConnectSuccess) {
                this.username = username;
                System.out.println(((UserConnectSuccess) result).msg);
            } else {
                System.out.println(((UserConnectFailure) result).msg);
            }
        } catch (Exception e) {
            System.out.println("server is offline!");
        }
    }

    private void disconnect() {
        Future<Object> rt = Patterns.ask(managingServer, new ManagingServer.Disconnect(username), timeout);
        try {
            Object result = Await.result(rt, timeout.duration());
            if (result instanceof UserDisconnectSuccess) {
                System.out.println(((UserDisconnectSuccess) result).msg);
            }
        } catch (Exception e) {
            System.out.println("server is offline!");
        }
    }

    private void text(String target, String msg) {
        ActorRef targetRef = fetchTargetRef(target);

        if (targetRef == ActorRef.noSender())
            return;

        targetRef.tell(new UserChatTextMessage(username, msg), getSelf());
    }

    private void file(String target, byte[] file) {
        ActorRef targetRef = fetchTargetRef(target);

        if (targetRef == ActorRef.noSender())
            return;

        targetRef.tell(new UserChatFileMessage(username, file), getSelf());
    }

    private ActorRef fetchTargetRef(String target) {
        ActorRef targetActorRef;

        Future<Object> rt = Patterns.ask(managingServer, new ManagingServer.FetchTargetUserRef(target), timeout);
        try {
            targetActorRef = (ActorRef) Await.result(rt, timeout.duration());
        } catch (Exception e) {
            System.out.println("server is offline!");
            return null;
        }

        if (targetActorRef == ActorRef.noSender())
            System.out.println(String.format("%s does not exist!", target));

        log.info(String.format("fetched path: %s", targetActorRef));

        return targetActorRef;
    }

    public static class ConnectControlMessage {
        String username;

        public ConnectControlMessage(String username) {
            this.username = username;
        }
    }

    public static class DisconnectControlMessage {
    }

    public static class TextControlMessage {
        String target;
        String msg;

        public TextControlMessage(String target, String msg) {
            this.target = target;
            this.msg = msg;
        }
    }

    public static class FileControlMessage {
        String target;
        byte[] file;

        public FileControlMessage(String target, byte[] file) {
            this.target = target;
            this.file = file;
        }
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

    public static class UserDisconnectSuccess implements Serializable {
        String msg;

        public UserDisconnectSuccess(String msg) {
            this.msg = msg;
        }
    }

    public static class UserChatTextMessage implements Serializable {
        String source;
        String message;

        public UserChatTextMessage(String source, String message) {
            this.source = source;
            this.message = message;
        }

        public String getMessage() {
            LocalDateTime now = LocalDateTime.now();
            String time = String.format("%d:%d", now.getHour(), now.getMinute());
            return String.format("[%s][user][%s]%s", time, source, message);
        }
    }

    public static class UserChatFileMessage implements Serializable {
        final static String message = "File received: %s";
        String source;
        byte[] file;

        public UserChatFileMessage(String source, byte[] file) {
            this.source = source;
            this.file = file;
        }

        public String getMessage(String targetFilePath) {
            LocalDateTime now = LocalDateTime.now();
            String time = String.format("%d:%d", now.getHour(), now.getMinute());
            return String.format("[%s][user][%s]%s", time, source, String.format(message, targetFilePath));
        }
    }
}