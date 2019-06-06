package Whatsapp.Messages;

public class UserCLIControlMessages {
    public static class ConnectControlMessage {
        public String username;

        public ConnectControlMessage(String username) {
            this.username = username;
        }
    }

    public static class GroupSendTextControlMessage {
        public final String groupName;
        public final String message;

        public GroupSendTextControlMessage(String groupName, String message) {
            this.groupName = groupName;
            this.message = message;
        }
    }

    public static class DisconnectControlMessage {
    }

    public static class CreateGroupControlMessage {
        public String groupname;

        public CreateGroupControlMessage(String groupname) {
            this.groupname = groupname;
        }
    }

    public static class LeaveGroupControlMessage {
        public String groupname;

        public LeaveGroupControlMessage(String groupname) {
            this.groupname = groupname;
        }
    }

    public static class GroupSendFileControlMessage {
        public final byte[] fileContant;
        public final String groupname;

        public GroupSendFileControlMessage(String groupname, byte[] fileContent) {
            this.groupname = groupname;
            this.fileContant = fileContent;
        }
    }

    public static class GroupAddCoAdminControlMessage {
        public String groupName;
        public String targetUsername;

        public GroupAddCoAdminControlMessage(String groupName, String targetUsername) {
            this.groupName = groupName;
            this.targetUsername = targetUsername;
        }
    }

    public static class GroupRemoveCoAdminControlMessage {
        public String groupName;
        public String targetUsername;

        public GroupRemoveCoAdminControlMessage(String groupName, String targetUsername) {
            this.groupName = groupName;
            this.targetUsername = targetUsername;
        }
    }

    public static class GroupInviteControlMessage {
        public String groupName;
        public String targetUsername;

        public GroupInviteControlMessage(String groupName, String targetUsername) {
            this.groupName = groupName;
            this.targetUsername = targetUsername;
        }
    }

    public static class TextControlMessage {
        public String target;
        public String msg;

        public TextControlMessage(String target, String msg) {
            this.target = target;
            this.msg = msg;
        }
    }

    public static class FileControlMessage {
        public String target;
        public byte[] file;

        public FileControlMessage(String target, byte[] file) {
            this.target = target;
            this.file = file;
        }
    }
}
