package se.kth.id2203.beb;

import se.sics.kompics.PortType;

public class BebPort extends PortType {
	
    {
        indication(BebDeliver.class);
        request(BebRequest.class);
    }
}
