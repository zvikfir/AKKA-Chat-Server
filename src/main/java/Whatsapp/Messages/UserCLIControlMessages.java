package Whatsapp.Messages;

/**
 * This class contains messages that are sent to the ChatActor as means to
 * perform commands that were given as input from the client via the UserCLI
 */
public class UserCLIControlMessages {
    /**
     * /user connect <username>
     */
    public static class UserConnectControlMessage {
        public final String userName;

        public UserConnectControlMessage(String userName) {
            this.userName = userName;
        }
    }

    /**
     * /user disconnect
     */
    public static class UserDisconnectControlMessage {
    }

    /**
     * /user file <targetusername> <filePath>
     */
    public static class UserFileControlMessage {
        public final String target;
        public final byte[] file;

        public UserFileControlMessage(String target, byte[] file) {
            this.target = target;
            this.file = file;
        }
    }

    /**
     * /user text <targetusername> <message>
     */
    public static class UserTextControlMessage {
        public final String target;
        public final String msg;

        public UserTextControlMessage(String target, String msg) {
            this.target = target;
            this.msg = msg;
        }
    }

    /**
     * /group coadmin add <groupname> <targetusername>
     */
    public static class GroupCoAdminAddControlMessage {
        public final String groupName;
        public final String targetUserName;

        public GroupCoAdminAddControlMessage(String groupName, String targetUserName) {
            this.groupName = groupName;
            this.targetUserName = targetUserName;
        }
    }

    /**
     * /group coadmin remove <groupname> <targetusername>
     */
    public static class GroupCoAdminRemoveControlMessage {
        public final String groupName;
        public final String targetUserName;

        public GroupCoAdminRemoveControlMessage(String groupName, String targetUserName) {
            this.groupName = groupName;
            this.targetUserName = targetUserName;
        }
    }

    /**
     * /group create <groupname>
     */
    public static class GroupCreateControlMessage {
        public final String groupName;

        public GroupCreateControlMessage(String groupName) {
            this.groupName = groupName;
        }
    }

    /**
     * /group leave <groupname>
     */
    public static class GroupLeaveControlMessage {
        public final String groupName;

        public GroupLeaveControlMessage(String groupName) {
            this.groupName = groupName;
        }
    }

    /**
     * /group send file <groupname> <filePath>
     */
    public static class GroupSendFileControlMessage {
        public final byte[] fileContent;
        public final String groupName;

        public GroupSendFileControlMessage(String groupName, byte[] fileContent) {
            this.groupName = groupName;
            this.fileContent = fileContent;
        }
    }

    /**
     * /group send text <groupname> <message>
     */
    public static class GroupSendTextControlMessage {
        public final String groupName;
        public final String message;

        public GroupSendTextControlMessage(String groupName, String message) {
            this.groupName = groupName;
            this.message = message;
        }
    }

    /**
     * /group user invite <groupname> <targetusername>
     */
    public static class GroupUserInviteControlMessage {
        public final String groupName;
        public final String targetUserName;

        public GroupUserInviteControlMessage(String groupName, String targetUserName) {
            this.groupName = groupName;
            this.targetUserName = targetUserName;
        }
    }

    /**
     * /group user remove <groupname> <targetusername>
     */
    public static class GroupUserRemoveControlMessage {
        public final String groupName;
        public final String targetUserName;

        public GroupUserRemoveControlMessage(String groupName, String targetUserName) {
            this.groupName = groupName;
            this.targetUserName = targetUserName;
        }
    }

    /**
     * /group user mute <groupname> <targetusername>
     */
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

    /**
     * /group user unmute <groupname> <targetusername>
     */
    public static class GroupUserUnmuteControlMessage {
        public final String groupName;
        public final String targetUserName;

        public GroupUserUnmuteControlMessage(String groupName, String targetUserName) {
            this.groupName = groupName;
            this.targetUserName = targetUserName;
        }
    }

    /**
     * A message sent as an acceptance to join a group after receiving an invitation to one
     * y|Y|Yes|yes
     */
    public static class GroupUserInviteAcceptControlMessage {
    }

    /**
     * A message sent as a decline to join a group after receiving an invitation to one
     * n|N|No|no
     */
    public static class GroupUserInviteDeclineControlMessage {
    }
}
