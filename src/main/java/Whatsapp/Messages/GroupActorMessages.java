package Whatsapp.Messages;

import akka.actor.ActorRef;

import java.io.Serializable;

/**
 * This class contains messages that, as a rule of thumb, should be handled by the GroupActor
 */
public class GroupActorMessages {

    /**
     * A message received when a user tries to add a co-admin to the group.
     * The group should authenticate the user by its username and verify that
     * he has permission to do so, and act accordingly.
     */
    public static class AddCoAdminMessage implements Serializable {
        public final String groupName;
        public final String userName;
        public final String targetUserName;
        public final ActorRef targetUserActorRef;

        public AddCoAdminMessage(String groupName, String userName, String targetUserName,
                                 ActorRef targetUserActorRef) {
            this.groupName = groupName;
            this.userName = userName;
            this.targetUserName = targetUserName;
            this.targetUserActorRef = targetUserActorRef;
        }
    }

    /**
     * A message received when a user tries to remove a co-admin from the group.
     * The group should authenticate the user by its username and verify that
     * he has permission to do so, and act accordingly.
     */
    public static class RemoveCoAdminMessage implements Serializable {
        public final String groupName;
        public final String userName;
        public final String targetUserName;
        public final ActorRef targetUserActorRef;

        public RemoveCoAdminMessage(String groupName, String userName, String targetUserName,
                                    ActorRef targetUserActorRef) {
            this.groupName = groupName;
            this.userName = userName;
            this.targetUserName = targetUserName;
            this.targetUserActorRef = targetUserActorRef;
        }
    }

    /**
     * A message received from a user willing to create a new group.
     * The message contains the username which will act as the group admin
     */
    public static class CreateGroupMessage implements Serializable {
        public final String userName;
        public final String groupName;

        public CreateGroupMessage(String userName, String groupName) {
            this.userName = userName;
            this.groupName = groupName;
        }
    }

    /**
     * A message received when a user is willing to leave the group.
     * After handling the message, the requesting user should no longer be
     * a member of the group, meaning he will no longer be an admin/co-admin or muted,
     * and he will not be able to send or receive messages from the group
     */
    public static class LeaveGroupMessage implements Serializable {
        public final String userName;
        public final String groupName;

        public LeaveGroupMessage(String userName, String groupName) {
            this.userName = userName;
            this.groupName = groupName;
        }
    }

    /**
     * A message received from a user willing to validate that he has permission
     * to invite a new user to the group and sending him a group invitation request.
     * It means that he should be an admin or a co-admin, and that the invitee
     * should not already be in the group
     */
    public static class ValidateInviteMessage implements Serializable {
        public final String groupName;
        public final String targetUserName;
        public final String userName;

        public ValidateInviteMessage(String groupName, String targetUserName, String userName) {
            this.groupName = groupName;
            this.targetUserName = targetUserName;
            this.userName = userName;
        }
    }

    /**
     * A message received from an admin or a co-admin willing to remove a member
     * of the group from the group.
     */
    public static class RemoveUserMessage implements Serializable {
        public final String sourceUserName;
        public final String targetUserName;
        public final ActorRef targetActorRef;
        public final String groupName;

        public RemoveUserMessage(String sourceUserName, String targetUserName, ActorRef targetActorRef,
                                 String groupName) {
            this.sourceUserName = sourceUserName;
            this.targetUserName = targetUserName;
            this.targetActorRef = targetActorRef;
            this.groupName = groupName;
        }
    }

    /**
     * A message received from an admin/co-admin willing to mute another member
     * of the group. The message contains the time the other member should be
     * muted for in seconds, and once the time is up, the member is automatically
     * un-muted. Once muted, a user can't send messages to the group
     */
    public static class MuteUserMessage implements Serializable {
        public final String userName;
        public final String targetUserName;
        public final ActorRef targetActorRef;
        public final long timeInSeconds;
        public final String groupName;

        public MuteUserMessage(String userName, String targetUserName, ActorRef targetActorRef, long timeInSeconds,
                               String groupName) {
            this.userName = userName;
            this.targetUserName = targetUserName;
            this.targetActorRef = targetActorRef;
            this.timeInSeconds = timeInSeconds;
            this.groupName = groupName;
        }
    }

    /**
     * A message received by the scheduler of the GroupActor set when an
     * MuteUserMessage is received. The scheduler sends the message after
     * the mute-time is over automatically.
     */
    public static class AutoUnmuteMessage implements Serializable {
        public final String userName;
        public final ActorRef targetRef;

        public AutoUnmuteMessage(String userName, ActorRef targetRef) {
            this.userName = userName;
            this.targetRef = targetRef;
        }
    }

    /**
     * A message received from an admin/co-admin willing to un-mute a member
     * of the group. Once the member is un-muted, he can send messages to the group.
     */
    public static class UnmuteUserMessage implements Serializable {
        public final String userName;
        public final String targetUserName;
        public final ActorRef targetRef;
        public final String groupName;

        public UnmuteUserMessage(String userName, String targetUserName, ActorRef targetRef, String groupName) {
            this.userName = userName;
            this.targetUserName = targetUserName;
            this.targetRef = targetRef;
            this.groupName = groupName;
        }
    }
}
