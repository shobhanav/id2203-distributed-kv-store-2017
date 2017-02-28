package se.kth.id2203.epfd;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;


public class Restore implements KompicsEvent {

    private NetAddress node;

    public Restore(NetAddress node) {
        this.node = node;
    }

    public NetAddress getNode() {
        return this.node;
    }
}
