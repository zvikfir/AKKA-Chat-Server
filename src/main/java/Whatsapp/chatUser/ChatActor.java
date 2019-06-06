package Whatsapp.chatUser;

import Whatsapp.Messages.GroupActorMessages;
import Whatsapp.Messages.ManagingActorMessages;
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
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import Whatsapp.Messages.ChatActorMessages.*;
import Whatsapp.Messages.UserCLIControlMessages.*;

public class ChatActor extends AbstractActor {
    private static final String MANAGING_SERVER_ADDRESS = "akka://Whatsapp@127.0.0.1:2552/user/managingServer";
    private final static Timeout timeout = new Timeout(Duration.create(1, TimeUnit.SECONDS));
    private final ActorSelection managingServer = getContext().actorSelection(MANAGING_SERVER_ADDRESS);
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private String username;
    private HashMap<String, ActorRef> groups = new HashMap<String, ActorRef>();

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
                .match(UserChatFileMessage.class, msg -> fileReceived(msg.file, msg.getMessage()))
                .match(CreateGroupControlMessage.class, msg -> createGroup(msg.groupname))
                .match(LeaveGroupControlMessage.class, msg -> leaveGroup(msg.groupname))
                .match(ManagingMessage.class, msg -> log.info(msg.msg))
                .match(GroupSendTextControlMessage.class, msg -> textGroup(msg.groupName, msg.message))
                .match(UserChatGroupTextMessage.class, msg -> log.info(msg.getMessage()))
                .match(GroupSendFileControlMessage.class, msg -> managingServer.tell(new UserChatGroupFileMessage(username, msg.groupname, msg.fileContant), getSelf()))
                .match(UserChatGroupFileMessage.class, msg -> fileReceived(msg.fileContent, msg.getMessage()))
                .match(GroupAddCoAdminControlMessage.class, this::groupAddCoAdmin)
                .match(GroupRemoveCoAdminControlMessage.class, this::groupRemoveCoAdmin)
                .match(GroupInviteControlMessage.class, this::groupValidateInvite)
                .match(GroupActorMessages.ValidateGroupInviteMessage.class, this::groupInvite)
                .match(GroupInviteConfirmation.class, this::groupInviteConfirmation)
                .build();
    }

    private void groupInviteConfirmation(GroupInviteConfirmation msg) {
        Scanner in = new Scanner(System.in);
        String answer = in.nextLine();
        getSender().tell(answer, getSelf());

//        lastTime;
//        while(currentTime - lastTime < 1000)
//        {}
//        getSelf().tell(new getInput());


    }

    private void groupValidateInvite(GroupInviteControlMessage msg) {
        managingServer.tell(new GroupActorMessages.ValidateGroupInviteMessage(msg.groupName, msg.targetUsername, username), getSelf());
    }

    private void groupInvite(GroupActorMessages.ValidateGroupInviteMessage msg) {
        ActorRef targetUsernameActorRef = fetchUserRef(msg.targetUsername);
        if (targetUsernameActorRef == ActorRef.noSender())
            return;
        Future<Object> rt = Patterns.ask(targetUsernameActorRef, new GroupInviteConfirmation(), new Timeout(Duration.create(60, TimeUnit.SECONDS)));
        rt.onComplete(result -> {
            if (result.toString().toLowerCase().equals("yes"))
                managingServer.tell(new GroupActorMessages.GroupInviteMessage(msg.groupName, username, msg.targetUsername, targetUsernameActorRef), getSelf());
            return "group invite future";
        }, getContext().dispatcher());
    }

    private void groupRemoveCoAdmin(GroupRemoveCoAdminControlMessage msg) {
        ActorRef targetUsernameActorRef = fetchUserRef(msg.targetUsername);
        if (targetUsernameActorRef == ActorRef.noSender())
            return;

        managingServer.tell(new GroupActorMessages.GroupRemoveCoAdmin(msg.groupName, username, msg.targetUsername, targetUsernameActorRef), getSelf());
    }

    private void groupAddCoAdmin(GroupAddCoAdminControlMessage msg) {
        ActorRef targetUsernameActorRef = fetchUserRef(msg.targetUsername);
        if (targetUsernameActorRef == ActorRef.noSender())
            return;

        managingServer.tell(new GroupActorMessages.GroupAddCoAdmin(msg.groupName, username, msg.targetUsername, targetUsernameActorRef), getSelf());
    }

    private void leaveGroup(String groupname) {
        Future<Object> rt = Patterns.ask(groups.get(groupname), new GroupActorMessages.GroupLeaveMessage(username, getSelf())
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
        managingServer.tell(new GroupActorMessages.GroupCreateMessage(username, groupname), getSelf());
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
        Future<Object> rt = Patterns.ask(managingServer, new ManagingActorMessages.UserConnectMessage(username, getSelf()),
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
        Future<Object> rt = Patterns.ask(managingServer, new ManagingActorMessages.UserDisconnectMessage(username), timeout);
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

        Future<Object> rt = Patterns.ask(managingServer, new ManagingActorMessages.FetchTargetUserRef(target), timeout);
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
        managingServer.tell(new UserChatGroupTextMessage(username, groupName, msg), getSelf());
    }





}