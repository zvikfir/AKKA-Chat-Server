package Whatsapp.Messages;

import akka.actor.ActorRef;

import java.io.Serializable;

public class GroupActorMessages {
    public static class GroupLeaveMessage implements Serializable {
        final String username;
        final ActorRef sourcePath;

        public GroupLeaveMessage(String username, ActorRef sourcePath) {
            this.username = username;
            this.sourcePath = sourcePath;
        }
    }

    public static class GroupCreateMessage implements Serializable {
        public final String username;
        public final String groupname;

        public GroupCreateMessage(String username, String groupname) {
            this.username = username;
            this.groupname = groupname;
        }
    }

    public static class GroupInviteMessage implements Serializable {
        public String groupName;
        public String username;
        public String targetUsername;
        public ActorRef targetUserActorRef;

        public GroupInviteMessage(String groupName, String username, String targetUsername, ActorRef targetUserActorRef) {
            this.groupName = groupName;
            this.username = username;
            this.targetUsername = targetUsername;
            this.targetUserActorRef = targetUserActorRef;
        }
    }

    public static class GroupAddCoAdmin implements Serializable {
        public String groupName;
        public String username;
        public String targetUsername;
        public ActorRef targetUserActorRef;

        public GroupAddCoAdmin(String groupName, String username, String targetUsername, ActorRef targetUserActorRef) {
            this.groupName = groupName;
            this.username = username;
            this.targetUsername = targetUsername;
            this.targetUserActorRef = targetUserActorRef;
        }
    }

    public static class GroupRemoveCoAdmin implements Serializable {
        public String groupName;
        public String username;
        public String targetUsername;
        public ActorRef targetUserActorRef;

        public GroupRemoveCoAdmin(String groupName, String username, String targetUsername, ActorRef targetUserActorRef) {
            this.groupName = groupName;
            this.username = username;
            this.targetUsername = targetUsername;
            this.targetUserActorRef = targetUserActorRef;
        }
    }

    public static class ValidateGroupInviteMessage implements Serializable {
        public final String groupName;
        public final String targetUsername;
        public final String username;

        public ValidateGroupInviteMessage(String groupName, String targetUsername, String username) {
            this.groupName = groupName;
            this.targetUsername = targetUsername;
            this.username = username;
        }
    }
}
