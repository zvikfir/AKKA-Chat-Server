package Whatsapp.Messages;

import akka.actor.ActorRef;

import java.io.Serializable;

/**
 * This class contains messages that, as a rule of thumb, should be handled by the ManagingActor
 */
public class ManagingActorMessages {

    /**
     * A message received by a user willing to connect to the server.
     * The server should verify that the username is not taken by any
     * other user, and then send a message confirming the connection.
     */
    public static class UserConnectMessage implements Serializable {
        public final String userName;
        public final ActorRef sourcePath;

        public UserConnectMessage(String userName, ActorRef sourcePath) {
            this.userName = userName;
            this.sourcePath = sourcePath;
        }
    }

    /**
     * A message received by a user willing to disconnect from the server.
     * The user's information should be discarded entirely. Each group that
     * included the user should be informed and delete any record of the user.
     * The server sends a notification indicating a successful disconnect.
     */
    public static class UserDisconnectMessage implements Serializable {
        public final String userName;
        public final ActorRef userRef;

        public UserDisconnectMessage(String userName, ActorRef userRef) {
            this.userName = userName;
            this.userRef = userRef;
        }
    }

    /**
     * A message received by a user, requesting the ActorRef of another,
     * already connected, user. This message is sent so that each user will
     * be able to communicate with other users directly, and not via the server.
     */
    public static class FetchTargetUserRefMessage implements Serializable {
        public final String target;
        public final ActorRef targetRef;

        public FetchTargetUserRefMessage(String target, ActorRef targetRef) {
            this.target = target;
            this.targetRef = targetRef;
        }
    }

    /**
     * A message sent by the GroupActor, in order notify the managing server
     * of a group that is deleted. The managing server should delete any record of the group,
     * and allow users to re-create a group with the same name.
     */
    public static class GroupDeleteMessage implements Serializable {
        public final String groupName;

        public GroupDeleteMessage(String groupName) {
            this.groupName = groupName;
        }
    }
}
