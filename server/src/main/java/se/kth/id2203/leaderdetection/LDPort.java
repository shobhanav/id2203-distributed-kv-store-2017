package se.kth.id2203.leaderdetection;

import se.sics.kompics.PortType;

public class LDPort extends PortType {
    {
        indication(Trust.class);
        request(LeaderDetectionAssignment.class);
    }
}
