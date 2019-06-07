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
    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private String username;
    private Stack<AskToJoinMessage> groupsInvitations = new Stack<AskToJoinMessage>();

    static public Props props() {
        return Props.create(ChatActor.class, ChatActor::new);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(UserConnectControlMessage.class, msg -> connect(msg.username))
                .match(UserDisconnectControlMessage.class, msg -> disconnect())
                .match(UserTextControlMessage.class, msg -> text(msg.target, msg.msg))
                .match(UserFileControlMessage.class, msg -> file(msg.target, msg.file))
                .match(UserChatTextMessage.class, msg -> log.info(msg.getMessage()))
                .match(UserChatFileMessage.class, msg -> fileReceived(msg.file, msg.getMessage()))
                .match(GroupCreateControlMessage.class, msg -> createGroup(msg.groupname))
                .match(GroupLeaveControlMessage.class, msg -> leaveGroup(msg.groupname))
                .match(ManagingMessage.class, msg -> log.info(msg.msg))
                .match(GroupSendTextControlMessage.class, msg -> textGroup(msg.groupName, msg.message))
                .match(UserChatGroupTextMessage.class, msg -> log.info(msg.getMessage()))
                .match(GroupSendFileControlMessage.class,
                        msg -> managingServer.tell(new UserChatGroupFileMessage(username, msg.groupname,
                                msg.fileContant), getSelf()))
                .match(UserChatGroupFileMessage.class, msg -> fileReceived(msg.fileContent, msg.getMessage()))
                .match(GroupCoadminAddControlMessage.class, this::groupAddCoAdmin)
                .match(GroupCoadminRemoveControlMessage.class, this::groupRemoveCoAdmin)
                .match(GroupUserInviteControlMessage.class, this::validateGroupInvitePermission)
                .match(GroupActorMessages.ValidateGroupInviteMessage.class, this::askUserToJoinGroup)
                .match(AskToJoinMessage.class, this::saveAndPrintInvitation)
                .match(GroupUserInviteAcceptControlMessage.class, this::handlePendingGroupInvitations)
                .match(GroupUserInviteDeclineControlMessage.class, msg -> {
                    if (groupsInvitations.empty()) {
                        log.info("There are no available groups invitations");
                        return;
                    }
                    AskToJoinMessage lastInvite = groupsInvitations.pop();
                    ActorRef inviter = fetchUserRef(lastInvite.inviter);
                    if (inviter == null)
                        return;

                    inviter.tell(new ManagingMessage(String.format("%s declined invitation to group %s", username,
                            lastInvite.groupName)), getSelf());

                    if (!groupsInvitations.empty()) {
                        printGroupInvitation(groupsInvitations.peek());
                    }
                })
                .match(GroupInvitationAccepted.class, msg -> {
                    managingServer.forward(msg, getContext());
                    getSender().tell(new ManagingMessage(String.format("Welcome to %s!", msg.groupName)), getSelf());
                })
                .match(GroupUserRemoveControlMessage.class, this::removeUser)
                .match(GroupUserMuteControlMessage.class, msg -> {
                    ActorRef targetRef = fetchUserRef(msg.targetUsername);
                    if (targetRef == null)
                        return;
                    managingServer.tell(new GroupActorMessages.GroupUserMute(username, msg.targetUsername, targetRef,
                            msg.timeInSeconds, msg.groupName), getSelf());
                })
                .match(GroupUserUnmuteControlMessage.class, msg -> {
                    ActorRef targetRef = fetchUserRef(msg.targetUsername);
                    if (targetRef == null)
                        return;
                    managingServer.tell(new GroupActorMessages.GroupUserUnmute(username, msg.targetUsername,
                            targetRef, msg.groupName), getSelf());
                })
                .build();
    }

    private void printGroupInvitation(AskToJoinMessage msg) {
        log.info(String.format("You have been invited to %s, Accept?", msg.groupName));
    }

    private void handlePendingGroupInvitations(GroupUserInviteAcceptControlMessage msg) {
        if (groupsInvitations.empty()) {
            log.info("There are no available groups invitations");
            return;
        }
        AskToJoinMessage lastInvite = groupsInvitations.pop();
        ActorRef inviter = fetchUserRef(lastInvite.inviter);
        inviter.tell(new GroupInvitationAccepted(lastInvite.groupName, username), getSelf());

        if (!groupsInvitations.empty()) {
            printGroupInvitation(groupsInvitations.peek());
        }
    }

    private void saveAndPrintInvitation(AskToJoinMessage msg) {
        if (groupsInvitations.empty()) {
            printGroupInvitation(msg);
        }
        groupsInvitations.push(msg);
    }

    private void validateGroupInvitePermission(GroupUserInviteControlMessage msg) {
        managingServer.tell(new GroupActorMessages.ValidateGroupInviteMessage(msg.groupName, msg.targetUsername,
                username), getSelf());
    }

    private void askUserToJoinGroup(GroupActorMessages.ValidateGroupInviteMessage msg) {
        ActorRef targetUsernameActorRef = fetchUserRef(msg.targetUsername);
        if (targetUsernameActorRef == null)
            return;

        targetUsernameActorRef.tell(new AskToJoinMessage(msg.groupName, username), getSelf());
    }

    private void groupRemoveCoAdmin(GroupCoadminRemoveControlMessage msg) {
        ActorRef targetUsernameActorRef = fetchUserRef(msg.targetUsername);
        if (targetUsernameActorRef == ActorRef.noSender())
            return;

        managingServer.tell(new GroupActorMessages.GroupRemoveCoAdmin(msg.groupName, username, msg.targetUsername,
                targetUsernameActorRef), getSelf());
    }

    private void groupAddCoAdmin(GroupCoadminAddControlMessage msg) {
        ActorRef targetUsernameActorRef = fetchUserRef(msg.targetUsername);
        if (targetUsernameActorRef == ActorRef.noSender())
            return;

        managingServer.tell(new GroupActorMessages.GroupAddCoAdmin(msg.groupName, username, msg.targetUsername,
                targetUsernameActorRef), getSelf());
    }

    private void leaveGroup(String groupname) {
        managingServer.tell(new GroupActorMessages.GroupLeaveMessage(username, groupname), getSelf());
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
        Future<Object> rt = Patterns.ask(managingServer, new ManagingActorMessages.UserConnectMessage(username,
                        getSelf()),
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
        Future<Object> rt = Patterns.ask(managingServer, new ManagingActorMessages.UserDisconnectMessage(username,
                getSelf()), timeout);
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

        Future<Object> rt = Patterns.ask(managingServer, new ManagingActorMessages.FetchTargetUserRef(target, null),
                timeout);
        try {
            targetActorRef =
                    ((ManagingActorMessages.FetchTargetUserRef) Await.result(rt, timeout.duration())).targetRef;
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

    private void removeUser(GroupUserRemoveControlMessage msg) {
        ActorRef targetActorRef = fetchUserRef(msg.targetUserName);
        if (targetActorRef == null)
            return;
        managingServer.tell(new GroupActorMessages.GroupRemoveUserMessage(username, msg.targetUserName,
                targetActorRef, msg.groupName), getSelf());


    }
}