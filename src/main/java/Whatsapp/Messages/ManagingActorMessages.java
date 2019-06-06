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

        public FetchTargetUserRef(String target) {
            this.target = target;
        }
    }

    public static class UserDisconnectMessage implements Serializable {
        public final String username;

        public UserDisconnectMessage(String username) {
            this.username = username;
        }
    }

    public static class GroupDeleteMessage implements Serializable {
        public final String groupname;

        public GroupDeleteMessage(String groupname) {
            this.groupname = groupname;
        }
    }
}
