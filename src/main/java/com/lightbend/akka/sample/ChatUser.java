package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Scanner;

public class ChatUser extends AbstractActor {

    public static class ChatMessage {
        String groupName;
        String sourceUserName;
        String message;

        public String getMessage() {
            LocalDateTime now = LocalDateTime.now();
            String time = String.format("%d:%d", now.getHour(), now.getMinute());
            return String.format("[%s][%s][%s]%s", time, groupName, sourceUserName, message);
        }

        public ChatMessage(String groupName, String sourceUserName, String message) {
            this.groupName = groupName;
            this.sourceUserName = sourceUserName;
            this.message = message;
        }
    }

    String username;

    public ChatUser(String username) {
        this.username = username;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ChatMessage.class, msg -> {
                    System.out.println(msg.getMessage());
                })
                .build();
    }

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("Whatsapp");
        final ActorSelection managingServer = system.actorSelection("/system/user/ManagingServer");

        Scanner in = new Scanner(System.in);

        System.out.print("Enter username:");
        String username = in.nextLine();
        System.out.print("Username is - " + username);

//        managingServer.ask();

    }
}
