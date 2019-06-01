package Whatsapp.chatUser;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
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
            // /user connect <username>
            case "create":
                user.tell(new ChatActor.CreateGroupControlMessage(cmd_parts[2]), ActorRef.noSender());
                break;
        }
    }

    private static void cli_user(ActorRef user, String[] cmd_parts) {
        String cmd = cmd_parts[1];
        switch (cmd) {
            // /user connect <username>
            case "connect":
                user.tell(new ChatActor.ConnectControlMessage(cmd_parts[2]), ActorRef.noSender());
                break;
            // /user disconnect
            case "disconnect":
                user.tell(new ChatActor.DisconnectControlMessage(), ActorRef.noSender());
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
        byte[] fileContent;
        try {
            fileContent = Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            System.out.println(String.format("%s does not exist!", filePath));
            return;
        }

        user.tell(new ChatActor.FileControlMessage(target, fileContent), ActorRef.noSender());
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
}
