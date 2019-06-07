package Whatsapp.managingServer;

import Whatsapp.Messages.ChatActorMessages;
import Whatsapp.Messages.GroupActorMessages;
import Whatsapp.Messages.ManagingActorMessages.FetchTargetUserRef;
import Whatsapp.Messages.ManagingActorMessages.GroupDeleteMessage;
import Whatsapp.Messages.ManagingActorMessages.UserConnectMessage;
import Whatsapp.Messages.ManagingActorMessages.UserDisconnectMessage;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.util.HashMap;


public class ManagingActor extends AbstractActor {
    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private HashMap<String, ActorRef> users;
    private HashMap<String, ActorRef> groups;

    public ManagingActor() {
        this.users = new HashMap<String, ActorRef>();
        this.groups = new HashMap<String, ActorRef>();
    }

    static public Props props() {
        return Props.create(ManagingActor.class, ManagingActor::new);
    }

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("Whatsapp", ConfigFactory.load("managingServer"));

        try {
            final ActorRef managingServerActor = system.actorOf(ManagingActor.props(), "managingServer");

            System.out.println(">>> Press ENTER to exit <<<");
            System.in.read();

        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        } finally {
            system.terminate();
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(UserConnectMessage.class, this::connectRequest)
                .match(FetchTargetUserRef.class, this::fetchTargetRequest)
                .match(UserDisconnectMessage.class, this::disconnectRequest)
                .match(GroupActorMessages.GroupCreateMessage.class, this::groupCreateRequest)
                .match(GroupActorMessages.GroupAddCoAdmin.class, msg -> groupForward(msg.groupName, msg))
                .match(GroupActorMessages.GroupRemoveCoAdmin.class, msg -> groupForward(msg.groupName, msg))
                .match(GroupDeleteMessage.class, this::deleteGroup)
                .match(ChatActorMessages.UserChatGroupTextMessage.class, msg -> groupForward(msg.groupName, msg))
                .match(ChatActorMessages.UserChatGroupFileMessage.class, msg -> groupForward(msg.groupName, msg))
                .match(GroupActorMessages.ValidateGroupInviteMessage.class, msg -> groupForward(msg.groupName, msg))
                .match(ChatActorMessages.GroupInvitationAccepted.class, msg -> groupForward(msg.groupName, msg))
                .match(GroupActorMessages.GroupLeaveMessage.class, msg -> groupForward(msg.groupName, msg))
                .match(GroupActorMessages.GroupRemoveUserMessage.class, msg -> groupForward(msg.groupName, msg))
                .match(GroupActorMessages.GroupUserMute.class, msg -> groupForward(msg.groupName, msg))
                .match(GroupActorMessages.GroupUserUnmute.class, msg -> groupForward(msg.groupName, msg))
                .build();
    }

    private void groupForward(String groupName, Object msg) {
        if (!groups.containsKey(groupName)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s does not exist!", groupName)),
                    getSelf());
        } else {
            groups.get(groupName).forward(msg, getContext());
        }
    }

    private void deleteGroup(GroupDeleteMessage groupDeleteMessage) {
        getContext().stop(groups.get(groupDeleteMessage.groupName));
        groups.remove(groupDeleteMessage.groupName);
    }

    private void groupCreateRequest(GroupActorMessages.GroupCreateMessage groupCreate) {
        if (groups.containsKey(groupCreate.groupname)) {
            getSender().tell(new ChatActorMessages.GroupCreateFailure(
                    String.format("%s already exists!", groupCreate.groupname)), getSelf());
        } else {
            ActorRef groupActor = getContext().actorOf(GroupActor.props(groupCreate.groupname), groupCreate.groupname);
            this.groups.put(groupCreate.groupname, groupActor);
            groupActor.forward(groupCreate, getContext());
        }
    }

    private void connectRequest(UserConnectMessage connect) {
        if (users.containsKey(connect.username)) {
            getSender().tell(new ChatActorMessages.UserConnectFailure(
                    String.format("%s is in use!", connect.username)), getSelf());
        } else {
            this.users.put(connect.username, connect.sourcePath);
            getSender().tell(new ChatActorMessages.UserConnectSuccess(
                    String.format("%s has connected successfully!", connect.username)), getSelf());
        }
    }

    private void disconnectRequest(UserDisconnectMessage disconnect) {
        this.users.remove(disconnect.username);
        getSender().tell(new ChatActorMessages.UserDisconnectSuccess(
                String.format("%s has been disconnected successfully!", disconnect.username)), getSelf());

        groups.values().forEach(ref -> ref.forward(disconnect, getContext()));
    }

    private void fetchTargetRequest(FetchTargetUserRef fetchTarget) {
        ActorRef target = null;
        if (users.containsKey(fetchTarget.target)) {
            target = users.get(fetchTarget.target);
        }
        getSender().tell(new FetchTargetUserRef(fetchTarget.target, target), getSelf());
    }


}
