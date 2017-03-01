package se.kth.id2203;

import com.google.common.base.Optional;

import se.kth.id2203.beb.BebComponent;
import se.kth.id2203.beb.BebPort;
import se.kth.id2203.bootstrapping.BootstrapClient;
import se.kth.id2203.bootstrapping.BootstrapServer;
import se.kth.id2203.bootstrapping.Bootstrapping;
import se.kth.id2203.epfd.EventuallyPerfectFailureDetector;
import se.kth.id2203.epfd.FDPort;
import se.kth.id2203.kvstore.KVService;
import se.kth.id2203.kvstore.ReplicationPort;
import se.kth.id2203.leaderdetection.LDPort;
import se.kth.id2203.leaderdetection.MonarchicalEventualLeaderDetector;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.Routing;
import se.kth.id2203.overlay.VSOverlayManager;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

public class ParentComponent
        extends ComponentDefinition {

    //******* Ports ******
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    //******* Children ******
    protected final Component overlay = create(VSOverlayManager.class, Init.NONE);
    protected final Component kv = create(KVService.class, Init.NONE);
    protected final Component epfd = create(EventuallyPerfectFailureDetector.class, Init.NONE);
    protected final Component ld = create(MonarchicalEventualLeaderDetector.class, Init.NONE);
    protected final Component beb = create(BebComponent.class, Init.NONE);
    protected final Component boot;

    {

        Optional<NetAddress> serverO = config().readValue("id2203.project.bootstrap-address", NetAddress.class);
        if (serverO.isPresent()) { // start in client mode
            boot = create(BootstrapClient.class, Init.NONE);
        } else { // start in server mode
            boot = create(BootstrapServer.class, Init.NONE);
        }
        connect(timer, boot.getNegative(Timer.class), Channel.TWO_WAY);
        connect(net, boot.getNegative(Network.class), Channel.TWO_WAY);
        // Overlay
        connect(boot.getPositive(Bootstrapping.class), overlay.getNegative(Bootstrapping.class), Channel.TWO_WAY);
        connect(net, overlay.getNegative(Network.class), Channel.TWO_WAY);
        connect(beb.getPositive(BebPort.class), overlay.getNegative(BebPort.class), Channel.TWO_WAY);
        
        // KV
        connect(overlay.getPositive(Routing.class), kv.getNegative(Routing.class), Channel.TWO_WAY);
        connect(net, kv.getNegative(Network.class), Channel.TWO_WAY);
        connect(kv.getPositive(ReplicationPort.class),boot.getNegative(ReplicationPort.class), Channel.TWO_WAY);
        connect(ld.getPositive(LDPort.class), kv.getNegative(LDPort.class), Channel.TWO_WAY);
        connect(beb.getPositive(BebPort.class), kv.getNegative(BebPort.class), Channel.TWO_WAY);
        
        //epfd
        connect(epfd.getPositive(FDPort.class), overlay.getNegative(FDPort.class), Channel.TWO_WAY);
        connect(timer, epfd.getNegative(Timer.class), Channel.TWO_WAY);
        connect(net, epfd.getNegative(Network.class), Channel.TWO_WAY);
        
        //leader detector
        connect(ld.getPositive(LDPort.class), overlay.getNegative(LDPort.class), Channel.TWO_WAY);
        connect(epfd.getPositive(FDPort.class), ld.getNegative(FDPort.class), Channel.TWO_WAY);       
        
        //beb
        connect(net, beb.getNegative(Network.class), Channel.TWO_WAY);
    }
}
