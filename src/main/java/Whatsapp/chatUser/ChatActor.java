package Whatsapp.chatUser;

import Whatsapp.managingServer.GroupActor;
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
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class ChatActor extends AbstractActor {
    public static final String MANAGING_SERVER_ADDRESS = "akka://Whatsapp@127.0.0.1:2552/user/managingServer";
    final static Timeout timeout = new Timeout(Duration.create(1, TimeUnit.SECONDS));
    final ActorSelection managingServer = getContext().actorSelection(MANAGING_SERVER_ADDRESS);
    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    String username;
    HashMap<String, ActorRef> groups = new HashMap<String, ActorRef>();

    static public Props props() {
        return Props.create(ChatActor.class, ChatActor::new);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(UserCLI.ConnectControlMessage.class, msg -> connect(msg.username))
                .match(UserCLI.DisconnectControlMessage.class, msg -> disconnect())
                .match(TextControlMessage.class, msg -> text(msg.target, msg.msg))
                .match(FileControlMessage.class, msg -> file(msg.target, msg.file))
                .match(UserChatTextMessage.class, msg -> log.info(msg.getMessage()))
                .match(UserChatFileMessage.class, msg -> fileReceived(msg.file, msg.getMessage()))
                .match(UserCLI.CreateGroupControlMessage.class, msg -> createGroup(msg.groupname))
                .match(UserCLI.LeaveGroupControlMessage.class, msg -> leaveGroup(msg.groupname))
                .match(ManagingMessage.class, msg -> log.info(msg.msg))
                .match(UserCLI.GroupSendTextControlMessage.class, msg -> textGroup(msg.groupName, msg.message))
                .match(GroupActor.UserChatGroupTextMessage.class, msg -> log.info(msg.getMessage()))
                .match(UserCLI.GroupSendFileControlMessage.class, msg -> managingServer.tell(new GroupActor.UserChatGroupFileMessage(username, msg.groupname, msg.fileContant), getSelf()))
                .match(GroupActor.UserChatGroupFileMessage.class, msg -> fileReceived(msg.fileContent, msg.getMessage()))
                .build();
    }

    private void leaveGroup(String groupname) {
        Future<Object> rt = Patterns.ask(groups.get(groupname), new GroupActor.GroupLeaveMessage(username, getSelf())
                , timeout);
        groups.remove(groupname);
        try {
            Object result = Await.result(rt, timeout.duration());
            if (result instanceof GroupLeaveError) {
                log.info(((GroupLeaveError) result).msg);
            }
        } catch (Exception e) {
            log.info("server is offline!");
        }
    }

    private void createGroup(String groupname) {
        managingServer.tell(new GroupActor.GroupCreateMessage(username, groupname), getSelf());
    }

    private void fileReceived(byte[] file, String msg) {
        try {
            File tmpFile = File.createTempFile("chatUser-file", ".tmp");
            String targetFilePath = tmpFile.getAbsolutePath();
            FileOutputStream out = new FileOutputStream(targetFilePath);
            out.write(file);
            out.close();
            log.info(String.format(msg, targetFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connect(String username) {
        Future<Object> rt = Patterns.ask(managingServer, new ManagingActor.UserConnectMessage(username, getSelf()),
                timeout);
        try {
            Object result = Await.result(rt, timeout.duration());
            if (result instanceof UserConnectSuccess) {
                this.username = username;
                log.info(((UserConnectSuccess) result).msg);
            } else {
                log.info(((UserConnectFailure) result).msg);
            }
        } catch (Exception e) {
            log.info("server is offline!");
        }
    }

    private void disconnect() {
        Future<Object> rt = Patterns.ask(managingServer, new ManagingActor.UserDisconnectMessage(username), timeout);
        try {
            Object result = Await.result(rt, timeout.duration());
            if (result instanceof UserDisconnectSuccess) {
                log.info(((UserDisconnectSuccess) result).msg);
            }
        } catch (Exception e) {
            log.info("server is offline!");
        }
    }

    private void text(String target, String msg) {
        ActorRef targetRef = fetchUserRef(target);

        if (targetRef == ActorRef.noSender())
            return;

        targetRef.tell(new UserChatTextMessage(username, msg), getSelf());
    }

    private void file(String target, byte[] file) {
        ActorRef targetRef = fetchUserRef(target);

        if (targetRef == ActorRef.noSender())
            return;

        targetRef.tell(new UserChatFileMessage(username, file), getSelf());
    }

    private ActorRef fetchUserRef(String target) {
        ActorRef targetActorRef;

        Future<Object> rt = Patterns.ask(managingServer, new ManagingActor.FetchTargetUserRef(target), timeout);
        try {
            targetActorRef = (ActorRef) Await.result(rt, timeout.duration());
        } catch (Exception e) {
            log.info("server is offline!");
            return null;
        }

        if (targetActorRef == ActorRef.noSender())
            log.info(String.format("%s does not exist!", target));

//        log.info(String.format("fetched path: %s", targetActorRef));

        return targetActorRef;
    }

    private void textGroup(String groupName, String msg) {
        managingServer.tell(new GroupActor.UserChatGroupTextMessage(username, groupName, msg), getSelf());
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

        public String getMessage() {
            LocalDateTime now = LocalDateTime.now();
            String time = String.format("%d:%d", now.getHour(), now.getMinute());
            return String.format("[%s][user][%s]%s", time, source, message);
        }
    }

    public static class GroupCreateFailure implements Serializable {
        String msg;

        public GroupCreateFailure(String msg) {
            this.msg = msg;
        }
    }

    public static class GroupLeaveError implements Serializable {
        String msg;

        public GroupLeaveError(String msg) {
            this.msg = msg;
        }
    }

    public static class ManagingMessage implements Serializable {
        String msg;

        public ManagingMessage(String msg) {
            this.msg = msg;
        }
    }

}