package se.kth.id2203.beb;

import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;


import java.util.ArrayList;

public class BebRequest implements KompicsEvent {
    
    private final String request; //put, cas
    private final int key;
    private final int value;
    private final ArrayList <NetAddress> nodes;

    public BebRequest(String req, int key,  int value, ArrayList <NetAddress> nodes) {
        this.request = req;
        this.key = key;
        this.value = value;
        this.nodes = nodes;
    }

    public String getRequest() {
        return request;
    }
    
    public int getValue(){
    	return value;
    }
    
    public int getKey(){
    	return key;
    }

    public ArrayList<NetAddress> getBroadcastNodes() {
        return nodes;
    }
}