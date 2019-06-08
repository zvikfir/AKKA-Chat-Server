package Whatsapp.Messages;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * This class contains messages that, as a rule of thumb, should be handled by the GroupActor
 */
public class ChatActorMessages {

    /**
     * A message that is printed in the client, which contains information about actions that has been done,
     * or about errors that have come up during the process
     */
    public static class ManagingMessage implements Serializable {
        public final String msg;

        public ManagingMessage(String msg) {
            this.msg = msg;
        }
    }

    /**
     * A message containing a binary information. When a ChatActor receives it,
     * it retrieves the binary information from the message and saves it to a
     * temporary location on the client's computer
     */
    public static class FileMessage implements Serializable {
        public final static String message = "File received: %s";
        public final String source;
        public final byte[] file;

        public FileMessage(String source, byte[] file) {
            this.source = source;
            this.file = file;
        }

        public String getMessage() {
            LocalDateTime now = LocalDateTime.now();
            String time = String.format("%d:%d", now.getHour(), now.getMinute());
            return String.format("[%s][user][%s]%s", time, source, message);
        }
    }

    /**
     * A message containing text from another user. The ChatActor handles it
     * by printing the text it contains
     */
    public static class TextMessage implements Serializable {
        public final String source;
        public final String message;

        public TextMessage(String source, String message) {
            this.source = source;
            this.message = message;
        }

        public String getMessage() {
            LocalDateTime now = LocalDateTime.now();
            String time = String.format("%d:%d", now.getHour(), now.getMinute());
            return String.format("[%s][user][%s]%s", time, source, message);
        }
    }

    /**
     * A message containing text message sent via a group from a certain user.
     * The ChatActor prints this message when receiving it.
     */
    public static class GroupTextMessage implements Serializable {

        public final String userName;
        public final String groupName;
        public final String msg;

        public GroupTextMessage(String userName, String groupName, String msg) {
            this.userName = userName;
            this.groupName = groupName;
            this.msg = msg;
        }

        public String getMessage() {
            LocalDateTime now = LocalDateTime.now();
            String time = String.format("%d:%d", now.getHour(), now.getMinute());
            return String.format("[%s][%s][%s]%s", time, groupName, userName, msg);
        }
    }

    /**
     * A message containing a binary information sent via a group from a certain user.
     * The ChatActor saves the binary information to a temporary file in each member's computer,
     * and prints the location it is saved to.
     */
    public static class GroupFileMessage implements Serializable {
        final static String message = "File received: %s";
        public final byte[] fileContent;
        public final String groupName;
        public final String userName;


        public GroupFileMessage(String userName, String groupName, byte[] fileContent) {
            this.userName = userName;
            this.groupName = groupName;
            this.fileContent = fileContent;

        }

        public String getMessage() {
            LocalDateTime now = LocalDateTime.now();
            String time = String.format("%d:%d", now.getHour(), now.getMinute());
            return String.format("[%s][%s][%s]%s", time, groupName, userName, message);
        }
    }

    /**
     * A message representing an invitation to a group.
     * This message is stored in a stack in the ChatActor, which enables him to
     * receive several invitations simultanously, and answer each one after the other.
     */
    public static class JoinGroupRequestMessage implements Serializable {
        public final String groupName;
        public final String inviter;

        public JoinGroupRequestMessage(String groupName, String inviter) {
            this.groupName = groupName;
            this.inviter = inviter;
        }
    }

    /**
     * A message representing a positive answer to a group invitation.
     * When a ChatActor receives it, it forwards it to the managing server,
     * and sends the invitee a welcome message
     */
    public static class JoinGroupAcceptMessage implements Serializable {
        public final String groupName;
        public final String invited;

        public JoinGroupAcceptMessage(String groupName, String invited) {
            this.groupName = groupName;
            this.invited = invited;
        }
    }
}
