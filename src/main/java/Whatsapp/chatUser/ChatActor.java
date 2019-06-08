package Whatsapp.chatUser;

import Whatsapp.Messages.ChatActorMessages.*;
import Whatsapp.Messages.GroupActorMessages;
import Whatsapp.Messages.ManagingActorMessages;
import Whatsapp.Messages.UserCLIControlMessages.*;
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
import java.util.Stack;
import java.util.concurrent.TimeUnit;

public class ChatActor extends AbstractActor {
    private static final String MANAGING_SERVER_ADDRESS = "akka://Whatsapp@127.0.0.1:2552/user/managingServer";
    private final static Timeout timeout = new Timeout(Duration.create(1, TimeUnit.SECONDS));
    private final ActorSelection managingServer = getContext().actorSelection(MANAGING_SERVER_ADDRESS);
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Stack<JoinGroupRequestMessage> groupsInvitations = new Stack<>();
    private String userName;

    static public Props props() {
        return Props.create(ChatActor.class, ChatActor::new);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(UserConnectControlMessage.class, msg -> {
                    if (this.userName != null) {
                        log.error(String.format("Already connected as %s!", this.userName));
                        return;
                    }

                    Future<Object> rt = Patterns.ask(managingServer,
                            new ManagingActorMessages.UserConnectMessage(msg.userName, getSelf()), timeout);
                    try {
                        ManagingMessage result = (ManagingMessage) Await.result(rt, timeout.duration());
                        if (result.msg.equals(String.format("%s has connected successfully!", msg.userName)))
                            this.userName = msg.userName;
                        log.info(result.msg);
                    } catch (Exception e) {
                        log.info("server is offline!");
                    }
                })
                .match(UserDisconnectControlMessage.class, msg -> {
                    if (this.userName == null) {
                        log.error("You are not connected to the server!");
                        return;
                    }
                    Future<Object> rt = Patterns.ask(managingServer,
                            new ManagingActorMessages.UserDisconnectMessage(userName, getSelf()), timeout);
                    try {
                        Object result = Await.result(rt, timeout.duration());
                        log.info(((ManagingMessage) result).msg);
                        this.userName = null;
                    } catch (Exception e) {
                        log.info("server is offline!");
                    }
                })
                .match(UserTextControlMessage.class, msg -> {
                    ActorRef targetRef = fetchUserRef(msg.target);
                    if (targetRef == null)
                        return;

                    targetRef.tell(new TextMessage(userName, msg.msg), getSelf());
                })
                .match(UserFileControlMessage.class, msg -> {
                    ActorRef targetRef = fetchUserRef(msg.target);
                    if (targetRef == null)
                        return;

                    targetRef.tell(new FileMessage(userName, msg.file), getSelf());
                })
                .match(GroupCreateControlMessage.class, msg -> managingServer.tell(
                        new GroupActorMessages.CreateGroupMessage(userName, msg.groupName), getSelf()))
                .match(GroupLeaveControlMessage.class, msg -> managingServer.tell(
                        new GroupActorMessages.LeaveGroupMessage(userName, msg.groupName), getSelf()))
                .match(GroupSendTextControlMessage.class, msg -> managingServer.tell(new GroupTextMessage(userName,
                        msg.groupName, msg.message), getSelf()))
                .match(GroupSendFileControlMessage.class, msg -> managingServer.tell(new GroupFileMessage(userName,
                        msg.groupName, msg.fileContent), getSelf()))
                .match(GroupCoAdminAddControlMessage.class, msg -> {
                    ActorRef targetUserNameActorRef = fetchUserRef(msg.targetUserName);
                    if (targetUserNameActorRef == null)
                        return;

                    managingServer.tell(new GroupActorMessages.AddCoAdminMessage(msg.groupName, userName,
                            msg.targetUserName, targetUserNameActorRef), getSelf());
                })
                .match(GroupCoAdminRemoveControlMessage.class, msg -> {
                    ActorRef targetUserNameActorRef = fetchUserRef(msg.targetUserName);
                    if (targetUserNameActorRef == null)
                        return;

                    managingServer.tell(new GroupActorMessages.RemoveCoAdminMessage(msg.groupName, userName,
                            msg.targetUserName, targetUserNameActorRef), getSelf());
                })
                .match(GroupUserInviteControlMessage.class,
                        msg -> managingServer.tell(new GroupActorMessages.ValidateInviteMessage(msg.groupName,
                                msg.targetUserName, userName), getSelf()))
                .match(GroupUserInviteAcceptControlMessage.class, msg -> {
                    if (groupsInvitations.empty()) {
                        log.info("There are no available groups invitations");
                        return;
                    }

                    JoinGroupRequestMessage lastInvite = groupsInvitations.pop();
                    ActorRef inviter = fetchUserRef(lastInvite.inviter);
                    if (inviter == null)
                        return;

                    inviter.tell(new JoinGroupAcceptMessage(lastInvite.groupName, userName), getSelf());

                    if (!groupsInvitations.empty())
                        printGroupInvitation(groupsInvitations.peek());
                })
                .match(GroupUserInviteDeclineControlMessage.class, msg -> {
                    if (groupsInvitations.empty()) {
                        log.info("There are no available groups invitations");
                        return;
                    }
                    JoinGroupRequestMessage lastInvite = groupsInvitations.pop();
                    ActorRef inviter = fetchUserRef(lastInvite.inviter);
                    if (inviter == null)
                        return;

                    inviter.tell(new ManagingMessage(String.format("%s declined invitation to group %s", userName,
                            lastInvite.groupName)), getSelf());

                    if (!groupsInvitations.empty())
                        printGroupInvitation(groupsInvitations.peek());
                })
                .match(GroupUserRemoveControlMessage.class, msg -> {
                    ActorRef targetActorRef = fetchUserRef(msg.targetUserName);
                    if (targetActorRef == null)
                        return;

                    managingServer.tell(new GroupActorMessages.RemoveUserMessage(userName, msg.targetUserName,
                            targetActorRef, msg.groupName), getSelf());
                })
                .match(GroupUserMuteControlMessage.class, msg -> {
                    ActorRef targetRef = fetchUserRef(msg.targetUserName);
                    if (targetRef == null)
                        return;

                    managingServer.tell(new GroupActorMessages.MuteUserMessage(userName, msg.targetUserName,
                            targetRef, msg.timeInSeconds, msg.groupName), getSelf());
                })
                .match(GroupUserUnmuteControlMessage.class, msg -> {
                    ActorRef targetRef = fetchUserRef(msg.targetUserName);
                    if (targetRef == null)
                        return;

                    managingServer.tell(new GroupActorMessages.UnmuteUserMessage(userName, msg.targetUserName,
                            targetRef, msg.groupName), getSelf());
                })
                .match(TextMessage.class, msg -> log.info(msg.getMessage()))
                .match(FileMessage.class, msg -> saveFile(msg.file, msg.getMessage()))
                .match(ManagingMessage.class, msg -> log.info(msg.msg))
                .match(GroupTextMessage.class, msg -> log.info(msg.getMessage()))
                .match(GroupFileMessage.class, msg -> saveFile(msg.fileContent, msg.getMessage()))
                .match(GroupActorMessages.ValidateInviteMessage.class, msg -> {
                    ActorRef targetUserNameActorRef = fetchUserRef(msg.targetUserName);
                    if (targetUserNameActorRef == null)
                        return;

                    targetUserNameActorRef.tell(new JoinGroupRequestMessage(msg.groupName, userName), getSelf());
                })
                .match(JoinGroupRequestMessage.class, msg -> {
                    if (groupsInvitations.empty())
                        printGroupInvitation(msg);
                    groupsInvitations.push(msg);
                })
                .match(JoinGroupAcceptMessage.class, msg -> {
                    managingServer.forward(msg, getContext());
                    getSender().tell(new ManagingMessage(String.format("Welcome to %s!", msg.groupName)), getSelf());
                })
                .build();
    }

    private ActorRef fetchUserRef(String target) {
        ActorRef targetActorRef;

        Future<Object> rt = Patterns.ask(managingServer, new ManagingActorMessages.FetchTargetUserRefMessage(target,
                null), timeout);
        try {
            targetActorRef =
                    ((ManagingActorMessages.FetchTargetUserRefMessage) Await.result(rt, timeout.duration())).targetRef;
        } catch (Exception e) {
            log.info("server is offline!");
            return null;
        }

        if (targetActorRef == ActorRef.noSender())
            log.info(String.format("%s does not exist!", target));

        return targetActorRef;
    }

    private void saveFile(byte[] file, String msg) {
        try {
            File tmpFile = File.createTempFile("chatUser-sendFile", ".tmp");
            String targetFilePath = tmpFile.getAbsolutePath();
            FileOutputStream out = new FileOutputStream(targetFilePath);
            out.write(file);
            out.close();
            log.info(String.format(msg, targetFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printGroupInvitation(JoinGroupRequestMessage msg) {
        log.info(String.format("You have been invited to %s, Accept?", msg.groupName));
    }
}
