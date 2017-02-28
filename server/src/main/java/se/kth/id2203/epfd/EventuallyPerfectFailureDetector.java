package se.kth.id2203.epfd;



import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EventuallyPerfectFailureDetector extends ComponentDefinition {
	final static Logger LOG = LoggerFactory.getLogger(EventuallyPerfectFailureDetector.class);
    private ArrayList<NetAddress> nodes;
    private final ArrayList<NetAddress> suspected = new ArrayList<>();
    private final ArrayList<NetAddress> alive = new ArrayList<>();    
    private UUID timerId;
    private int heartbeatDelay = 10000;
    private int delta = 1000;
    Positive<Network> net = requires(Network.class);
    Positive<Timer> timer = requires(Timer.class);
    Negative<FDPort> epfd = provides(FDPort.class);
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);  

    protected final Handler<Start> startHandler = new Handler<Start>() {

        @Override
        public void handle(Start event) {
        }
    };


    protected final Handler<HeartbeatTimeout> heartbeatTimeoutHandler = new Handler<HeartbeatTimeout>() {

        @Override
        public void handle(HeartbeatTimeout heartbeatTimeout) {        	
        	LOG.info("Heartbeat timeout !");
            ArrayList<NetAddress> intersection = new ArrayList<>(alive);
            intersection.retainAll(suspected);
            //increment heartbeat delay if there is a false suspicion of an alive node
            if (!intersection.isEmpty()) {
                heartbeatDelay += delta;
            }

            Iterator it = nodes.iterator();
            while(it.hasNext()) {
                NetAddress node = (NetAddress) it.next();
                //suspect
                if (!alive.contains(node) && !suspected.contains(node)) {
                    suspected.add(node);
                   LOG.info("Suspect: {} ",node.toString());
                    trigger(new Suspect(node), epfd);
                }
                //restore
                else if (alive.contains(node) && suspected.contains(node)) {
                    suspected.remove(node);
                    LOG.info("Restored: {}", node.toString());
                    trigger(new Restore(node), epfd);
                }
                //Send a new heartbeat
                trigger(new Message(self, node, new HeartbeatRequest()), net);
            }
            alive.clear();
            //Subscribe to new timeout
            startTimer(heartbeatDelay);
        }
    };

    private void startTimer(int delay) {
    	ScheduleTimeout st = new ScheduleTimeout(delay);
        HeartbeatTimeout timeout = new HeartbeatTimeout(st);
        st.setTimeoutEvent(timeout);
        trigger(st, timer);
        timerId = timeout.getTimeoutId();
    }

    protected final ClassMatchedHandler<HeartbeatReply, Message> heartbeatReplyHandler = new ClassMatchedHandler<HeartbeatReply, Message>() {
        @Override
        public void handle(HeartbeatReply rep, Message ctx) {
            alive.add(ctx.getSource());
        }
    };

    protected final ClassMatchedHandler<HeartbeatRequest, Message> heartbeatRequestHandler = new ClassMatchedHandler<HeartbeatRequest, Message>() {
        @Override
        public void handle(HeartbeatRequest req, Message ctx ) {
            trigger(new Message(self, ctx.getSource(), new HeartbeatReply()), net);
        }
    };

    public class HeartbeatTimeout extends Timeout {
        public HeartbeatTimeout(ScheduleTimeout spt) {
            super(spt);
        }
    }
    
    protected final Handler<EpfdAssignment> assignmentHandler = new Handler<EpfdAssignment>() {

        @Override
        public void handle(EpfdAssignment event) {
           LOG.info("EPFD got the list of nodes to monitor {}", event.assignment);
           nodes = new ArrayList<NetAddress>(event.assignment);
           alive.addAll(nodes);           
           startTimer(heartbeatDelay);
        }
    };
    
    {
    	subscribe(startHandler, control);
        subscribe(heartbeatTimeoutHandler, timer);
        subscribe(heartbeatRequestHandler, net);
        subscribe(heartbeatReplyHandler, net);
        subscribe(assignmentHandler, epfd);
    }
}
