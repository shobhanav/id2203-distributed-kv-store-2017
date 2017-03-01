package se.kth.id2203.leaderdetection;

import se.kth.id2203.epfd.FDPort;
import se.kth.id2203.epfd.Restore;
import se.kth.id2203.epfd.Suspect;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.*;

import java.util.ArrayList;

public class MonarchicalEventualLeaderDetector extends ComponentDefinition {

    private ArrayList <NetAddress> suspected = new ArrayList<NetAddress>();
    private NetAddress leader;
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private Positive<FDPort> epfd = requires(FDPort.class);
    Negative<LDPort> ld = provides(LDPort.class);
    private ArrayList <NetAddress> nodes;

    Handler<Suspect> suspectHandler = new Handler<Suspect>() {
        @Override
        public void handle(Suspect suspect) {
            suspected.add(suspect.getNode());
            if(leader.equals(suspect.getNode())) {
                leader = maxRank(getAliveNodes());
                trigger(new Trust(leader), ld);
            }
        }
    };

    Handler<Restore> restoreHandler = new Handler<Restore>() {
        @Override
        public void handle(Restore restore) {
            suspected.remove(restore.getNode());
            NetAddress newLeader = maxRank(getAliveNodes());
            if(!leader.equals(newLeader)) {
                leader = newLeader;
                trigger(new Trust(leader), ld);
            }
        }
    };

    Handler<LeaderDetectionAssignment> assignmentHandler = new Handler<LeaderDetectionAssignment>() {
        @Override
        public void handle(LeaderDetectionAssignment event) {
        	nodes = new ArrayList<NetAddress>(event.assignment);        	
            leader = maxRank(getAliveNodes());
            trigger(new Trust(leader), ld);
        }
    };

    private ArrayList<NetAddress> getAliveNodes() {
        ArrayList<NetAddress> aliveNodes = new ArrayList<>();
        for(NetAddress node : nodes) {
            if(!suspected.contains(node)) {
                aliveNodes.add(node);
            }
        }
        return aliveNodes;
    }

    private NetAddress maxRank(ArrayList <NetAddress> nodes) {
        NetAddress newLeader = self;
        for(NetAddress node : nodes) {
            if(node.compareTo(newLeader) < 0 ) {
                newLeader = node;
            }
        }
        return newLeader;
    };
    
    {
        subscribe(suspectHandler, epfd);
        subscribe(restoreHandler, epfd);
        subscribe(assignmentHandler, ld);
    }


}
