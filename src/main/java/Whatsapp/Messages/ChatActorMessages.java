package Whatsapp.Messages;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ChatActorMessages {

    public static class UserConnectSuccess implements Serializable {
        public String msg;

        public UserConnectSuccess(String msg) {
            this.msg = msg;
        }
    }

    public static class UserConnectFailure implements Serializable {
        public String msg;

        public UserConnectFailure(String msg) {
            this.msg = msg;
        }
    }

    public static class UserDisconnectSuccess implements Serializable {
        public String msg;

        public UserDisconnectSuccess(String msg) {
            this.msg = msg;
        }
    }

    public static class UserChatTextMessage implements Serializable {
        public String source;
        public String message;

        public UserChatTextMessage(String source, String message) {
            this.source = source;
            this.message = message;
        }

        public String getMessage() {
            LocalDateTime now = LocalDateTime.now();
            String time = String.format("%d:%d", now.getHour(), now.getMinute());
            return String.format("[%s][user][%s]%s", time, source, message);
        }
    }

    public static class UserChatFileMessage implements Serializable {
        public final static String message = "File received: %s";
        public String source;
        public byte[] file;

        public UserChatFileMessage(String source, byte[] file) {
            this.source = source;
            this.file = file;
        }

        public String getMessage() {
            LocalDateTime now = LocalDateTime.now();
            String time = String.format("%d:%d", now.getHour(), now.getMinute());
            return String.format("[%s][user][%s]%s", time, source, message);
        }
    }

    public static class GroupCreateFailure implements Serializable {
        public String msg;

        public GroupCreateFailure(String msg) {
            this.msg = msg;
        }
    }

    public static class GroupLeaveError implements Serializable {
        public String msg;

        public GroupLeaveError(String msg) {
            this.msg = msg;
        }
    }

    public static class ManagingMessage implements Serializable {
        public String msg;

        public ManagingMessage(String msg) {
            this.msg = msg;
        }
    }

    public static class GroupInviteConfirmation implements Serializable {
    }

    public static class UserChatGroupTextMessage implements Serializable {

        public final String username;
        public final String groupName;
        public final String msg;

        public String getMessage() {
            LocalDateTime now = LocalDateTime.now();
            String time = String.format("%d:%d", now.getHour(), now.getMinute());
            return String.format("[%s][%s][%s]%s", time, groupName, username, msg);
        }

        public UserChatGroupTextMessage(String username, String groupName, String msg) {
            this.username = username;
            this.groupName = groupName;
            this.msg = msg;
        }
    }

    public static class UserChatGroupFileMessage implements Serializable {
        public final byte[] fileContent;
        public final String groupName;
        public final String username;
        final static String message = "File received: %s";


        public UserChatGroupFileMessage(String username, String groupname, byte[] fileContant) {
            this.username = username;
            this.groupName = groupname;
            this.fileContent = fileContant;

        }

        public String getMessage() {
            LocalDateTime now = LocalDateTime.now();
            String time = String.format("%d:%d", now.getHour(), now.getMinute());
            return String.format("[%s][%s][%s]%s", time, groupName, username, message);
        }
    }


}
