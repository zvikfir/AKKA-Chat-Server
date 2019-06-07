package Whatsapp.Messages;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ChatActorMessages {
    public static class ManagingMessage implements Serializable {
        public final String msg;

        public ManagingMessage(String msg) {
            this.msg = msg;
        }
    }

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

    public static class JoinGroupRequestMessage implements Serializable {
        public final String groupName;
        public final String inviter;

        public JoinGroupRequestMessage(String groupName, String inviter) {
            this.groupName = groupName;
            this.inviter = inviter;
        }
    }

    public static class JoinGroupAcceptMessage implements Serializable {
        public final String groupName;
        public final String invited;

        public JoinGroupAcceptMessage(String groupName, String invited) {
            this.groupName = groupName;
            this.invited = invited;
        }
    }
}
