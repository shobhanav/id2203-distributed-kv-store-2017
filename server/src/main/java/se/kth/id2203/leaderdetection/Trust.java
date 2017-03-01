package se.kth.id2203.leaderdetection;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

public class Trust implements KompicsEvent {
    private NetAddress leader;

    public Trust(NetAddress leader) {
        this.leader = leader;
    }

    public NetAddress getLeader() {
        return this.leader;
    }
}
