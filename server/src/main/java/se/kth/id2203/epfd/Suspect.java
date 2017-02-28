package se.kth.id2203.epfd;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;


public class Suspect implements KompicsEvent {

    private NetAddress node;

    public Suspect(NetAddress node) {
        this.node = node;
    }

    public NetAddress getNode() {
        return node;
    }
}
