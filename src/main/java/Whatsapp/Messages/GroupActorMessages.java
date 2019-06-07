package Whatsapp.Messages;

import akka.actor.ActorRef;

import java.io.Serializable;

public class GroupActorMessages {
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

    public static class CreateGroupMessage implements Serializable {
        public final String userName;
        public final String groupName;

        public CreateGroupMessage(String userName, String groupName) {
            this.userName = userName;
            this.groupName = groupName;
        }
    }

    public static class LeaveGroupMessage implements Serializable {
        public final String userName;
        public final String groupName;

        public LeaveGroupMessage(String userName, String groupName) {
            this.userName = userName;
            this.groupName = groupName;
        }
    }

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

    public static class AutoUnmuteMessage implements Serializable {
        public final String userName;
        public final ActorRef targetRef;

        public AutoUnmuteMessage(String userName, ActorRef targetRef) {
            this.userName = userName;
            this.targetRef = targetRef;
        }
    }

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
