package Whatsapp.Messages;

public class UserCLIControlMessages {
    public static class UserConnectControlMessage {
        public final String userName;

        public UserConnectControlMessage(String userName) {
            this.userName = userName;
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

    public static class GroupCoAdminAddControlMessage {
        public final String groupName;
        public final String targetUserName;

        public GroupCoAdminAddControlMessage(String groupName, String targetUserName) {
            this.groupName = groupName;
            this.targetUserName = targetUserName;
        }
    }

    public static class GroupCoAdminRemoveControlMessage {
        public final String groupName;
        public final String targetUserName;

        public GroupCoAdminRemoveControlMessage(String groupName, String targetUserName) {
            this.groupName = groupName;
            this.targetUserName = targetUserName;
        }
    }

    public static class GroupCreateControlMessage {
        public final String groupName;

        public GroupCreateControlMessage(String groupName) {
            this.groupName = groupName;
        }
    }

    public static class GroupLeaveControlMessage {
        public final String groupName;

        public GroupLeaveControlMessage(String groupName) {
            this.groupName = groupName;
        }
    }

    public static class GroupSendFileControlMessage {
        public final byte[] fileContent;
        public final String groupName;

        public GroupSendFileControlMessage(String groupName, byte[] fileContent) {
            this.groupName = groupName;
            this.fileContent = fileContent;
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
        public final String targetUserName;

        public GroupUserInviteControlMessage(String groupName, String targetUserName) {
            this.groupName = groupName;
            this.targetUserName = targetUserName;
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
        public final String targetUserName;
        public final long timeInSeconds;

        public GroupUserMuteControlMessage(String groupName, String targetUserName, long timeInSeconds) {
            this.groupName = groupName;
            this.targetUserName = targetUserName;
            this.timeInSeconds = timeInSeconds;
        }
    }

    public static class GroupUserUnmuteControlMessage {
        public final String groupName;
        public final String targetUserName;

        public GroupUserUnmuteControlMessage(String groupName, String targetUserName) {
            this.groupName = groupName;
            this.targetUserName = targetUserName;
        }
    }

    public static class GroupUserInviteAcceptControlMessage {
    }

    public static class GroupUserInviteDeclineControlMessage {
    }
}
