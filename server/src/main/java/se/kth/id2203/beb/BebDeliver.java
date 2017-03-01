package se.kth.id2203.beb;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

public class BebDeliver implements KompicsEvent {

    private NetAddress source;
    private Object data;

    public BebDeliver(NetAddress source, Object data) {
        this.source = source;
        this.data = data;
    }

    public NetAddress getSource() {
        return source;
    }

    public Object getData() {
        return data;
    }
}