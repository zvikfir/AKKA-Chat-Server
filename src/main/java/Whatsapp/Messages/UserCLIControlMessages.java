package Whatsapp.Messages;

public class UserCLIControlMessages {
    public static class UserConnectControlMessage {
        public final String username;

        public UserConnectControlMessage(String username) {
            this.username = username;
        }
    }

    public static class UserDisconnectControlMessage {
    }

    public static class UserFileControlMessage {
        public final String target;
        public final byte[] file;

        public UserFileControlMessage(String target, byte[] file) {
            this.target = target;
            this.file = file;
        }
    }

    public static class UserTextControlMessage {
        public final String target;
        public final String msg;

        public UserTextControlMessage(String target, String msg) {
            this.target = target;
            this.msg = msg;
        }
    }

    public static class GroupCoadminAddControlMessage {
        public final String groupName;
        public final String targetUsername;

        public GroupCoadminAddControlMessage(String groupName, String targetUsername) {
            this.groupName = groupName;
            this.targetUsername = targetUsername;
        }
    }

    public static class GroupCoadminRemoveControlMessage {
        public final String groupName;
        public final String targetUsername;

        public GroupCoadminRemoveControlMessage(String groupName, String targetUsername) {
            this.groupName = groupName;
            this.targetUsername = targetUsername;
        }
    }

    public static class GroupCreateControlMessage {
        public final String groupname;

        public GroupCreateControlMessage(String groupname) {
            this.groupname = groupname;
        }
    }

    public static class GroupLeaveControlMessage {
        public final String groupname;

        public GroupLeaveControlMessage(String groupname) {
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

    public static class GroupSendTextControlMessage {
        public final String groupName;

        public final String message;

        public GroupSendTextControlMessage(String groupName, String message) {
            this.groupName = groupName;
            this.message = message;
        }

    }

    public static class GroupUserInviteControlMessage {
        public final String groupName;
        public final String targetUsername;

        public GroupUserInviteControlMessage(String groupName, String targetUsername) {
            this.groupName = groupName;
            this.targetUsername = targetUsername;
        }
    }

    public static class GroupUserRemoveControlMessage {
        public final String groupName;
        public final String targetUserName;

        public GroupUserRemoveControlMessage(String groupName, String targetUserName) {
            this.groupName = groupName;
            this.targetUserName = targetUserName;
        }
    }

    public static class GroupUserMuteControlMessage {
        public final String groupName;
        public final String targetUsername;
        public final long timeInSeconds;

        public GroupUserMuteControlMessage(String groupName, String targetUsername, long timeInSeconds) {
            this.groupName = groupName;
            this.targetUsername = targetUsername;
            this.timeInSeconds = timeInSeconds;
        }
    }

    public static class GroupUserUnmuteControlMessage {
        public final String groupName;
        public final String targetUsername;

        public GroupUserUnmuteControlMessage(String groupName, String targetUsername) {
            this.groupName = groupName;
            this.targetUsername = targetUsername;
        }
    }

    public static class GroupUserInviteAcceptControlMessage {

    }

    public static class GroupUserInviteDeclineControlMessage {
    }
}
