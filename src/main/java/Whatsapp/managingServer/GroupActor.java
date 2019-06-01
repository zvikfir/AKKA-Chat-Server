package Whatsapp.managingServer;

import akka.actor.AbstractActor;
import akka.actor.Props;

public class GroupActor extends AbstractActor {
    static public Props props() {
        return Props.create(GroupActor.class, GroupActor::new);
    }

    @Override
    public Receive createReceive() {
        return null;
    }
}
