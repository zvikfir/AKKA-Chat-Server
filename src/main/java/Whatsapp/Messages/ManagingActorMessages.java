package Whatsapp.Messages;

import akka.actor.ActorRef;

import java.io.Serializable;

public class ManagingActorMessages {
    public static class UserConnectMessage implements Serializable {
        public final String username;
        public final ActorRef sourcePath;

        public UserConnectMessage(String username, ActorRef sourcePath) {
            this.username = username;
            this.sourcePath = sourcePath;
        }
    }

    public static class FetchTargetUserRef implements Serializable {
        public final String target;
        public final ActorRef targetRef;

        public FetchTargetUserRef(String target, ActorRef targetRef) {
            this.target = target;
            this.targetRef = targetRef;
        }
    }

    public static class UserDisconnectMessage implements Serializable {
        public final String username;
        public final ActorRef userRef;

        public UserDisconnectMessage(String username, ActorRef userRef) {
            this.username = username;
            this.userRef = userRef;
        }
    }

    public static class GroupDeleteMessage implements Serializable {
        public final String groupName;

        public GroupDeleteMessage(String groupName) {
            this.groupName = groupName;
        }
    }
}
