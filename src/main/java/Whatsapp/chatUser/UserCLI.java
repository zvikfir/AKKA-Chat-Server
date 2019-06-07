package Whatsapp.chatUser;


import Whatsapp.Messages.UserCLIControlMessages.*;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.sun.deploy.util.StringUtils;
import com.typesafe.config.ConfigFactory;
import org.jline.builtins.Completers;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.jline.builtins.Completers.TreeCompleter.node;

public class UserCLI {

    // TODO: make this work!
    private static Completer getCompleter() {
        return new Completers.TreeCompleter(
                node("/user",
                        node("connect",
                                node("username")),
                        node("disconnect"),
                        node("file",
                                node("target", "sourcefilePath")),
                        node("text",
                                node("target", "message"))),
                node("/group",
                        node("coadmin",
                                node("add",
                                        node("groupname", "targetusername")),
                                node("remove",
                                        node("groupname", "targetusername"))),
                        node("create", node("groupname")),
                        node("leave", node("groupname")),
                        node("send",
                                node("file",
                                        node("groupname", "sourcefilePath")),
                                node("text",
                                        node("groupname", "message"))),
                        node("user",
                                node("invite",
                                        node("groupname", "targetusername")),
                                node("remove",
                                        node("groupname", "targetusername")),
                                node("mute",
                                        node("groupname", "targetgroupname", "timeinseconds")),
                                node("unmute",
                                        node("groupname", "targetusername")
                                ))));
    }

    private static String getText(List<String> words, int index) {
        return StringUtils.join(words.subList(index, words.size()), " ");
    }

    public static byte[] readFile(String filePath) {
        byte[] fileContent;
        try {
            fileContent = Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            System.out.println(String.format("%s does not exist!", filePath));
            return null;
        }
        return fileContent;
    }

    public static void main(String[] args) throws IOException {
        final ActorSystem system = ActorSystem.create("chatUser", ConfigFactory.load("chatUser"));
        final ActorRef user = system.actorOf(ChatActor.props(), "user");

        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(getCompleter())
                .build();

        while (true) {
            String line = reader.readLine(">> ");
            if (line == null) {
                continue;
            }
            line = line.trim();

            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit"))
                break;
            ParsedLine pl = reader.getParser().parse(line, 0);
            switch (pl.word().toLowerCase()) {
                case "/user":
                    if (pl.words().size() < 2)
                        continue;
                    switch (pl.words().get(1)) {
                        // /user connect <username>
                        case "connect":
                            if (pl.words().size() < 3)
                                continue;
                            user.tell(new UserConnectControlMessage(pl.words().get(2)), ActorRef.noSender());
                            break;
                        // /user disconnect
                        case "disconnect":
                            user.tell(new UserDisconnectControlMessage(), ActorRef.noSender());
                            break;
                        // /user file <target> <sourcefilePath>
                        case "file":
                            if (pl.words().size() < 4)
                                continue;
                            byte[] fileContent = readFile(pl.words().get(3));
                            if (fileContent == null)
                                continue;
                            user.tell(new UserFileControlMessage(pl.words().get(2), fileContent),
                                    ActorRef.noSender());
                            break;
                        // /user text <target> <message>
                        case "text":
                            if (pl.words().size() < 4)
                                continue;
                            user.tell(new UserTextControlMessage(pl.words().get(2),
                                    getText(pl.words(), 3)), ActorRef.noSender());
                            break;
                    }
                    break;
                case "/group":
                    if (pl.words().size() < 3)
                        continue;
                    switch (pl.words().get(1)) {
                        case "coadmin":
                            if (pl.words().size() < 5)
                                continue;
                            switch (pl.words().get(2)) {
                                // /group coadmin add <groupname> <targetusername>
                                case "add":
                                    user.tell(new GroupCoadminAddControlMessage(pl.words().get(3), pl.words().get(4))
                                            , ActorRef.noSender());
                                    break;
                                // /group coadmin remove <groupname> <targetusername>
                                case "remove":
                                    user.tell(new GroupCoadminRemoveControlMessage(pl.words().get(3),
                                            pl.words().get(4)), ActorRef.noSender());
                                    break;
                            }
                            break;
                        // /group create <groupname>
                        case "create":
                            user.tell(new GroupCreateControlMessage(pl.words().get(2)), ActorRef.noSender());
                            break;
                        // /group leave <groupname>
                        case "leave":
                            user.tell(new GroupLeaveControlMessage(pl.words().get(2)), ActorRef.noSender());
                            break;
                        case "send":
                            if (pl.words().size() < 5)
                                continue;
                            switch (pl.words().get(2)) {
                                // /group send file <groupname> <sourcefilePath>
                                case "file":
                                    byte[] fileContent = readFile(pl.words().get(4));
                                    if (fileContent == null)
                                        continue;
                                    user.tell(new GroupSendFileControlMessage(pl.words().get(3), fileContent),
                                            ActorRef.noSender());
                                    break;
                                // /group send text <groupname> <message>
                                case "text":
                                    user.tell(new GroupSendTextControlMessage(pl.words().get(3), getText(pl.words(),
                                            4)), ActorRef.noSender());
                                    break;
                            }
                            break;
                        case "user":
                            if (pl.words().size() < 5)
                                continue;
                            switch (pl.words().get(2)) {
                                // /group user invite <groupname> <targetusername>
                                case "invite":
                                    user.tell(new GroupUserInviteControlMessage(pl.words().get(3), pl.words().get(4)),
                                            ActorRef.noSender());
                                    break;
                                // /group user remove <groupname> <targetusername>
                                case "remove":
                                    user.tell(new GroupUserRemoveControlMessage(pl.words().get(3), pl.words().get(4)),
                                            ActorRef.noSender());
                                    break;
                                // /group user mute <groupname> <targetusername> <timeinseconds>
                                case "mute":
                                    if (pl.words().size() < 6)
                                        continue;
                                    user.tell(new GroupUserMuteControlMessage(pl.words().get(3), pl.words().get(4),
                                            Long.parseLong(pl.words().get(5))), ActorRef.noSender());
                                    break;
                                // /group user unmute <groupname> <targetusername>
                                case "unmute":
                                    user.tell(new GroupUserUnmuteControlMessage(pl.words().get(3), pl.words().get(4)),
                                            ActorRef.noSender());
                                    break;
                            }
                    }
                    break;
                case "yes":
                case "y":
                    user.tell(new GroupUserInviteAcceptControlMessage(), ActorRef.noSender());
                    break;
                case "no":
                case "n":
                    user.tell(new GroupUserInviteDeclineControlMessage(), ActorRef.noSender());
                    break;
                case "cls":
                    terminal.puts(InfoCmp.Capability.clear_screen);
                    terminal.flush();
                    break;
            }
        }

    }
}
