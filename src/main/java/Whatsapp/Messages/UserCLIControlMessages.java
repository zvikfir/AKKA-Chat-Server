package Whatsapp.Messages;

public class UserCLIControlMessages {
    public static class ConnectControlMessage {
        public final String username;

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
        public final String groupname;

        public CreateGroupControlMessage(String groupname) {
            this.groupname = groupname;
        }
    }

    public static class LeaveGroupControlMessage {
        public final String groupname;

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
        public final String groupName;
        public final String targetUsername;

        public GroupAddCoAdminControlMessage(String groupName, String targetUsername) {
            this.groupName = groupName;
            this.targetUsername = targetUsername;
        }
    }

    public static class GroupRemoveCoAdminControlMessage {
        public final String groupName;
        public final String targetUsername;

        public GroupRemoveCoAdminControlMessage(String groupName, String targetUsername) {
            this.groupName = groupName;
            this.targetUsername = targetUsername;
        }
    }

    public static class GroupInviteControlMessage {
        public final String groupName;
        public final String targetUsername;

        public GroupInviteControlMessage(String groupName, String targetUsername) {
            this.groupName = groupName;
            this.targetUsername = targetUsername;
        }
    }

    public static class TextControlMessage {
        public final String target;
        public final String msg;

        public TextControlMessage(String target, String msg) {
            this.target = target;
            this.msg = msg;
        }
    }

    public static class FileControlMessage {
        public final String target;
        public final byte[] file;

        public FileControlMessage(String target, byte[] file) {
            this.target = target;
            this.file = file;
        }
    }

    public static class AcceptGroupInvitationControlMessage {

    }

    public static class DeclineGroupInvitationControlMessage {}

    public static class RemoveUserControlMessage {
        public final String groupName;
        public final String targetUserName;

        public RemoveUserControlMessage(String groupName, String targetUserName) {
            this.groupName = groupName;
            this.targetUserName = targetUserName;
        }
    }

    public static class MuteUserControlMessage {
        public final String groupName;
        public final String targetUsername;
        public final long timeInSeconds;

        public MuteUserControlMessage(String groupName, String targetUsername, long timeInSeconds) {
            this.groupName = groupName;
            this.targetUsername = targetUsername;
            this.timeInSeconds = timeInSeconds;
        }
    }

    public static class UnmuteUserControlMessage {
        public final String groupName;
        public final String targetUsername;

        public UnmuteUserControlMessage(String groupName, String targetUsername) {
            this.groupName = groupName;
            this.targetUsername = targetUsername;
        }
    }

}
