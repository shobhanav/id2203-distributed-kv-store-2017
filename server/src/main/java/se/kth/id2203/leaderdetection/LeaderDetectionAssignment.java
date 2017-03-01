package se.kth.id2203.leaderdetection;

import java.util.Collection;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

public class LeaderDetectionAssignment implements KompicsEvent {
	
    public final Collection<NetAddress> assignment;

    public LeaderDetectionAssignment(final Collection<NetAddress> assignment) {
        this.assignment = assignment;
    }
}
