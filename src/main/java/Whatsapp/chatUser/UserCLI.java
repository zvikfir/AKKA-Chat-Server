package Whatsapp.chatUser;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class UserCLI {
    public static void cli(ActorRef user) {
        Scanner in = new Scanner(System.in);
        do {
            System.out.print(">>");
            String input = in.nextLine();

            if (input.startsWith("/user")) {
                String[] cmd_parts = input.split("\\s+");
                cli_user(user, cmd_parts);
            }
            if (input.startsWith("/group")) {
                String[] cmd_parts = input.split("\\s+");
                cli_group(user, cmd_parts);
            }
        } while (true);
    }

    private static void cli_group(ActorRef user, String[] cmd_parts) {
        String cmd = cmd_parts[1];
        switch (cmd) {
            // /group create <group-name>
            case "create":
                user.tell(new CreateGroupControlMessage(cmd_parts[2]), ActorRef.noSender());
                break;
            // /group leave <group-name>
            case "leave":
                user.tell(new LeaveGroupControlMessage(cmd_parts[2]), ActorRef.noSender());
                break;
            // /group send text <group-name> <message>
            case "send":
                cli_group_send(user, cmd_parts);
                break;
        }
        // /group send file <group-name> <sourceFilePath>
        // /group user invite <group-name> <targetUsername>
        // /group user remove <group-name> <targetUsername>
        // /group user mute <group-name> <targetUsername> <time-in-seconds>
        // /group user unmute <group-name> <targetUsername>
        // /group coadmin add <group-name> <targetUsername>
        // /group coadmin remove <group-name> <targetUsername>
    }

    private static void cli_user(ActorRef user, String[] cmd_parts) {
        String cmd = cmd_parts[1];
        switch (cmd) {
            // /user connect <username>
            case "connect":
                user.tell(new ConnectControlMessage(cmd_parts[2]), ActorRef.noSender());
                break;
            // /user disconnect
            case "disconnect":
                user.tell(new DisconnectControlMessage(), ActorRef.noSender());
                break;
            // /user text <target> <message>
            case "text":
                user.tell(new ChatActor.TextControlMessage(cmd_parts[2], cmd_parts[3]), ActorRef.noSender());
                break;
            // /user file <target> <sourceFilePath>
            case "file":
                cli_user_file(user, cmd_parts[2], cmd_parts[3]);
                break;
        }
    }

    public static void cli_user_file(ActorRef user, String target, String filePath) {
        byte[] fileContent = read_file(filePath);
        if (fileContent == null)
            return;

        user.tell(new ChatActor.FileControlMessage(target, fileContent), ActorRef.noSender());
    }

    public static byte[] read_file(String filePath) {
        byte[] fileContent;
        try {
            fileContent = Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            System.out.println(String.format("%s does not exist!", filePath));
            return null;
        }
        return fileContent;
    }

    private static void cli_group_send(ActorRef user, String[] cmd_parts) {
        switch (cmd_parts[2]) {
            case "text":
                user.tell(new GroupSendTextControlMessage(cmd_parts[3], cmd_parts[4]), ActorRef.noSender());
                break;
            case "file":
                byte[] fileContent = read_file(cmd_parts[4]);
                if (fileContent == null) {
                    return;
                }
                user.tell(new UserCLI.GroupSendFileControlMessage(cmd_parts[3], fileContent), ActorRef.noSender());
                break;
        }
    }

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("chatUser", ConfigFactory.load("chatUser"));
        try {
            final ActorRef user =
                    system.actorOf(ChatActor.props(), "user");
            cli(user);
        } catch (Exception ioe) {
            System.out.println(ioe.getMessage());
        } finally {
            system.terminate();
        }
    }

    public static class ConnectControlMessage {
        String username;

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
        String groupname;

        public CreateGroupControlMessage(String groupname) {
            this.groupname = groupname;
        }
    }

    public static class LeaveGroupControlMessage {
        String groupname;

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
}
