package Whatsapp.Messages;

import akka.actor.ActorRef;

import java.io.Serializable;

public class GroupActorMessages {
    public static class GroupLeaveMessage implements Serializable {
        public final String userName;
        public final String groupName;

        public GroupLeaveMessage(String userName, String groupName) {
            this.userName = userName;
            this.groupName = groupName;
        }
    }

    public static class GroupRemoveUserMessage implements Serializable {
        public final String sourceUserName;
        public final String targetUserName;
        public final ActorRef targetActorRef;
        public final String groupName;

        public GroupRemoveUserMessage(String sourceUserName, String targetUserName, ActorRef targetActorRef,
                                      String groupName) {
            this.sourceUserName = sourceUserName;
            this.targetUserName = targetUserName;
            this.targetActorRef = targetActorRef;
            this.groupName = groupName;
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

    public static class GroupAddCoAdmin implements Serializable {
        public final String groupName;
        public final String username;
        public final String targetUsername;
        public final ActorRef targetUserActorRef;

        public GroupAddCoAdmin(String groupName, String username, String targetUsername, ActorRef targetUserActorRef) {
            this.groupName = groupName;
            this.username = username;
            this.targetUsername = targetUsername;
            this.targetUserActorRef = targetUserActorRef;
        }
    }

    public static class GroupRemoveCoAdmin implements Serializable {
        public final String groupName;
        public final String username;
        public final String targetUsername;
        public final ActorRef targetUserActorRef;

        public GroupRemoveCoAdmin(String groupName, String username, String targetUsername,
                                  ActorRef targetUserActorRef) {
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

    public static class GroupUserMute implements Serializable {
        public final String username;
        public final String targetUsername;
        public final ActorRef targetActorRef;
        public final long timeInSeconds;
        public final String groupName;

        public GroupUserMute(String username, String targetUsername, ActorRef targetActorRef, long timeInSeconds,
                             String groupName) {
            this.username = username;
            this.targetUsername = targetUsername;
            this.targetActorRef = targetActorRef;
            this.timeInSeconds = timeInSeconds;
            this.groupName = groupName;
        }
    }

    public static class GroupAutoUnmute implements Serializable {
        public final String username;
        public final ActorRef targetRef;

        public GroupAutoUnmute(String username, ActorRef targetRef) {
            this.username = username;
            this.targetRef = targetRef;
        }
    }

    public static class GroupUserUnmute implements Serializable {
        public final String username;
        public final String targetUsername;
        public final ActorRef targetRef;
        public final String groupName;

        public GroupUserUnmute(String username, String targetUsername, ActorRef targetRef, String groupName) {
            this.username = username;
            this.targetUsername = targetUsername;
            this.targetRef = targetRef;
            this.groupName = groupName;
        }
    }
}
