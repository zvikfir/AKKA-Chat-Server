package Whatsapp.managingServer;

import Whatsapp.Messages.ChatActorMessages;
import Whatsapp.Messages.GroupActorMessages;
import Whatsapp.Messages.ManagingActorMessages.FetchTargetUserRefMessage;
import Whatsapp.Messages.ManagingActorMessages.GroupDeleteMessage;
import Whatsapp.Messages.ManagingActorMessages.UserConnectMessage;
import Whatsapp.Messages.ManagingActorMessages.UserDisconnectMessage;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.util.HashMap;


public class ManagingActor extends AbstractActor {

    private final HashMap<String, ActorRef> users;
    private final HashMap<String, ActorRef> groups;

    public ManagingActor() {
        this.users = new HashMap<>();
        this.groups = new HashMap<>();
    }

    static public Props props() {
        return Props.create(ManagingActor.class, ManagingActor::new);
    }

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("Whatsapp", ConfigFactory.load("managingServer"));

        try {
            system.actorOf(ManagingActor.props(), "managingServer");

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
                .match(UserConnectMessage.class, connect -> {
                    if (users.containsKey(connect.userName))
                        getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s is in use!",
                                connect.userName)), getSelf());
                    else {
                        this.users.put(connect.userName, connect.sourcePath);
                        getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s has connected " +
                                "successfully!", connect.userName)), getSelf());
                    }
                })
                .match(FetchTargetUserRefMessage.class, fetchTarget -> {
                    ActorRef target = null;
                    if (users.containsKey(fetchTarget.target)) {
                        target = users.get(fetchTarget.target);
                    }
                    getSender().tell(new FetchTargetUserRefMessage(fetchTarget.target, target), getSelf());
                })
                .match(UserDisconnectMessage.class, disconnect -> {
                    this.users.remove(disconnect.userName);
                    getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s has been disconnected " +
                            "successfully!", disconnect.userName)), getSelf());

                    // forwards the disconnect message to each group so they know to discard all the information
                    // about the disconnected user
                    groups.values().forEach(ref -> ref.forward(disconnect, getContext()));
                })
                .match(GroupActorMessages.CreateGroupMessage.class, groupCreate -> {
                    if (groups.containsKey(groupCreate.groupName)) {
                        getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s already exists!",
                                groupCreate.groupName)), getSelf());
                    } else {
                        ActorRef groupActor = getContext().actorOf(GroupActor.props(groupCreate.groupName),
                                groupCreate.groupName);
                        this.groups.put(groupCreate.groupName, groupActor);
                        groupActor.forward(groupCreate, getContext());
                    }
                })
                .match(GroupActorMessages.AddCoAdminMessage.class, msg -> groupForward(msg.groupName, msg))
                .match(GroupActorMessages.RemoveCoAdminMessage.class, msg -> groupForward(msg.groupName, msg))
                .match(GroupDeleteMessage.class, groupDeleteMessage -> {
                    getContext().stop(groups.get(groupDeleteMessage.groupName));
                    groups.remove(groupDeleteMessage.groupName);
                })
                .match(ChatActorMessages.GroupTextMessage.class, msg -> groupForward(msg.groupName, msg))
                .match(ChatActorMessages.GroupFileMessage.class, msg -> groupForward(msg.groupName, msg))
                .match(GroupActorMessages.ValidateInviteMessage.class, msg -> groupForward(msg.groupName, msg))
                .match(ChatActorMessages.JoinGroupAcceptMessage.class, msg -> groupForward(msg.groupName, msg))
                .match(GroupActorMessages.LeaveGroupMessage.class, msg -> groupForward(msg.groupName, msg))
                .match(GroupActorMessages.RemoveUserMessage.class, msg -> groupForward(msg.groupName, msg))
                .match(GroupActorMessages.MuteUserMessage.class, msg -> groupForward(msg.groupName, msg))
                .match(GroupActorMessages.UnmuteUserMessage.class, msg -> groupForward(msg.groupName, msg))
                .build();
    }

    /**
     * Forwards a message to the relevant group according the groupName
     *
     * @param groupName the name of the group to forward the message to
     * @param msg       the message to forward to the group
     */
    private void groupForward(String groupName, Object msg) {
        if (!groups.containsKey(groupName)) {
            getSender().tell(new ChatActorMessages.ManagingMessage(String.format("%s does not exist!", groupName)),
                    getSelf());
        } else {
            groups.get(groupName).forward(msg, getContext());
        }
    }
}
