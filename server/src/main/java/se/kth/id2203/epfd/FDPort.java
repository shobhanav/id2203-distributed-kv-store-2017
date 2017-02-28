package se.kth.id2203.epfd;

import se.sics.kompics.PortType;

public class FDPort extends PortType {
    {
        indication(Suspect.class);
        indication(Restore.class);
        request(EpfdAssignment.class);
    }
}
