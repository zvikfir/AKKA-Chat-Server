package com.lightbend.akka.sample;

import akka.actor.AbstractActor;
import akka.actor.Props;

import java.util.ArrayList;


public class ManagingServer extends AbstractActor {
    private ArrayList<String> userNames;

    public ManagingServer() {
        this.userNames = new ArrayList<String>();
    }

    static public Props props() {
        return Props.create(ManagingServer.class, () -> new ManagingServer());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Connect.class, connect -> {
                    if (userNames.contains(connect.userName)) {
                        getSender().tell("no", getSelf());
                    } else {
                        getSender().tell("ok", getSelf());
                        this.userNames.add(connect.userName);
                    }
                })
                .build();
    }

    static public class Connect {
        String userName;

        public Connect(String userName) {
            this.userName = userName;
        }
    }
}

