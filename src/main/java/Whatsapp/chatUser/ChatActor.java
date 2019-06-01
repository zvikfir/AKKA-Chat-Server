package Whatsapp.chatUser;

import Whatsapp.managingServer.ManagingActor;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class ChatActor extends AbstractActor {
    public static final String MANAGING_SERVER_ADDRESS = "akka://Whatsapp@127.0.0.1:2552/user/managingServer";
    final static Timeout timeout = new Timeout(Duration.create(1, TimeUnit.SECONDS));
    final ActorSelection managingServer = getContext().actorSelection(MANAGING_SERVER_ADDRESS);
    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    String username;

    static public Props props() {
        return Props.create(ChatActor.class, ChatActor::new);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ConnectControlMessage.class, msg -> connect(msg.username))
                .match(DisconnectControlMessage.class, msg -> disconnect())
                .match(TextControlMessage.class, msg -> text(msg.target, msg.msg))
                .match(FileControlMessage.class, msg -> file(msg.target, msg.file))
                .match(UserChatTextMessage.class, msg -> log.info(msg.getMessage()))
                .match(UserChatFileMessage.class, msg -> fileReceived(msg))
                .match(CreateGroupControlMessage.class, msg -> createGroup(msg.groupname))
                .build();
    }

    private void createGroup(String groupname) {
        Future<Object> rt = Patterns.ask(managingServer, new ManagingActor.GroupCrete(groupname, getSelf()), timeout);
        try {
            Object result = Await.result(rt, timeout.duration());
            if (result instanceof GroupCreateSuccess) {
                // TODO: Save the group name in some way
                System.out.println(((GroupCreateSuccess) result).msg);
            } else {
                System.out.println(((GroupCreateFailure) result).msg);
            }
        } catch (Exception e) {
            System.out.println("server is offline!");
        }
    }

    private void fileReceived(UserChatFileMessage msg) {
        try {
            File tmpFile = File.createTempFile("chatUser-file", ".tmp");
            String targetFilePath = tmpFile.getAbsolutePath();
            FileOutputStream out = new FileOutputStream(targetFilePath);
            out.write(msg.file);
            out.close();
            log.info(msg.getMessage(targetFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connect(String username) {
        Future<Object> rt = Patterns.ask(managingServer, new ManagingActor.Connect(username, getSelf()), timeout);
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
        Future<Object> rt = Patterns.ask(managingServer, new ManagingActor.Disconnect(username), timeout);
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

        Future<Object> rt = Patterns.ask(managingServer, new ManagingActor.FetchTargetUserRef(target), timeout);
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

    public static class CreateGroupControlMessage {
        String groupname;

        public CreateGroupControlMessage(String groupname) {
            this.groupname = groupname;
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

    public static class GroupCreateSuccess implements Serializable {
        String msg;

        public GroupCreateSuccess(String msg) {
            this.msg = msg;
        }
    }

    public static class GroupCreateFailure implements Serializable {
        String msg;

        public GroupCreateFailure(String msg) {
            this.msg = msg;
        }
    }
}