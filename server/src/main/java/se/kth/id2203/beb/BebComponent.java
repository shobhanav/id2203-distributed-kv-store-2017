package se.kth.id2203.beb;


import java.util.ArrayList;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.id2203.kvstore.KVService;

import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;

public class BebComponent extends ComponentDefinition {
	final static Logger LOG = LoggerFactory.getLogger(KVService.class);
	final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
	//******* Ports ******
    protected final Positive<Network> net = requires(Network.class);  
    protected final Negative<BebPort> beb = provides(BebPort.class);
    
    
    protected final Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {

        }
    };
    
    protected final Handler<BebRequest> reqHandler = new Handler<BebRequest>() {
		@Override
		public void handle(BebRequest event) {
			LOG.info("Beb: sending KV operation to all nodes in the replication group");
			ArrayList <NetAddress> nodes = event.getBroadcastNodes();
            for (NetAddress node : nodes) {
                trigger(new Message(self, node, new KVOperation(event.getRequest(), event.getKey(), event.getValue())), net);
            }			
		}   	
	};	
  	
	{
		subscribe(reqHandler, beb);
	}

  

}
