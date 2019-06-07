package Whatsapp.Messages;

import akka.actor.ActorRef;

import java.io.Serializable;

public class ManagingActorMessages {
    public static class UserConnectMessage implements Serializable {
        public final String userName;
        public final ActorRef sourcePath;

        public UserConnectMessage(String userName, ActorRef sourcePath) {
            this.userName = userName;
            this.sourcePath = sourcePath;
        }
    }

    public static class UserDisconnectMessage implements Serializable {
        public final String userName;
        public final ActorRef userRef;

        public UserDisconnectMessage(String userName, ActorRef userRef) {
            this.userName = userName;
            this.userRef = userRef;
        }
    }

    public static class FetchTargetUserRefMessage implements Serializable {
        public final String target;
        public final ActorRef targetRef;

        public FetchTargetUserRefMessage(String target, ActorRef targetRef) {
            this.target = target;
            this.targetRef = targetRef;
        }
    }

    public static class GroupDeleteMessage implements Serializable {
        public final String groupName;

        public GroupDeleteMessage(String groupName) {
            this.groupName = groupName;
        }
    }
}
