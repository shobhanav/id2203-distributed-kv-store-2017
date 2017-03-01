/*
 * The MIT License
 *
 * Copyright 2017 Lars Kroll <lkroll@kth.se>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.kth.id2203.kvstore;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.id2203.beb.BebPort;
import se.kth.id2203.beb.BebRequest;
import se.kth.id2203.beb.KVOperation;
import se.kth.id2203.bootstrapping.GetInitialAssignments;
import se.kth.id2203.bootstrapping.InitialAssignments;
import se.kth.id2203.epfd.FDPort;
import se.kth.id2203.kvstore.OpResponse.Code;
import se.kth.id2203.leaderdetection.LDPort;
import se.kth.id2203.leaderdetection.Trust;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.LookupTable;
import se.kth.id2203.overlay.Pair;
import se.kth.id2203.overlay.RouteMsg;
import se.kth.id2203.overlay.Routing;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class KVService extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(KVService.class);
    //******* Ports ******
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Routing> route = requires(Routing.class);
    protected final Negative<ReplicationPort> rep = provides(ReplicationPort.class);
    protected final Positive<LDPort> ld = requires(LDPort.class);
    protected final Positive<BebPort> beb = requires(BebPort.class);
    private NetAddress leader = null;
    
    private HashMap map;
    private int range_start =0;
	private int range_end = 0;
    
    //******* Fields ******
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    //******* Handlers ******
    protected final ClassMatchedHandler<Operation, Message> opHandler = new ClassMatchedHandler<Operation, Message>() {

        @Override
        public void handle(Operation content, Message context) {
            LOG.info("Got operation {}! ", content);
            int key = -100; 
            String[] arr;
            try{
            
            if(leader == null){
            	LOG.info("Leader detection not completed yet so ignoring request");
            	return;
            }
            
            if(leader.equals(self)){
            	arr = content.key.split(":");
            	if(arr == null || arr.length <2 || arr.length >3 ){
            		LOG.error("KVService: invalid command string");
            		return;
            	}else{
            		key = Integer.parseInt(arr[1]);
            		if(key >= range_start && key <=range_end){
            			if(arr[0].equals("get"))
            				trigger(new Message(self, context.getSource(), new OpResponse(content.id, Code.OK,(int)map.get(key))), net);
            			else if(arr[0].equals("put")){
            				int newVal = Integer.parseInt(arr[2]);
            				map.put(key, newVal);            				
            				//broadcast the new value to my group
            				trigger(new RouteMsg("put", new Pair(key, newVal)), route);
                			trigger(new Message(self, context.getSource(), new OpResponse(content.id, Code.OK, newVal)), net);
            			}else if(arr[0].equals("cas")){
            				int val = Integer.parseInt(arr[2]);
            				int newVal = Integer.parseInt(arr[3]);
            				int oldVal = (int) map.get(key);
            				if(val == oldVal){
            					map.put(key, newVal);
            					//broadcast the new value to my group
                				trigger(new RouteMsg("put", new Pair(key, newVal)), route);
                    			trigger(new Message(self, context.getSource(), new OpResponse(content.id, Code.OK, newVal)), net);
            				}else
            					trigger(new Message(self, context.getSource(), new OpResponse(content.id, Code.NOT_FOUND, newVal)), net);
            			}
            			else
            				trigger(new Message(self, context.getSource(), new OpResponse(content.id, Code.NOT_FOUND,0)), net);
                    }else{
                    	LOG.info("Leader: Key {} outside my key range! Ignoring request... ", key);
                    	return;
                    }            		
            	}
            }
            else{
            	LOG.info("Ignoring request as I am not the leader");
            	return;
            }           
                        	

            }catch(NumberFormatException e){
            	LOG.info("Key value store is of type <int, int>! ");
            }
        }

    };
    
    protected final Handler<ReplicationEvent> repHandler = new Handler<ReplicationEvent>() {

        @Override
        public void handle(ReplicationEvent event) {
            LOG.info("Let me sync up my KV store with the replication group");      
        }
    };
    
    protected final Handler<Trust> ldHandler = new Handler<Trust>() {
        @Override
        public void handle(Trust event) {
        	leader = event.getLeader();
        	LOG.info("My group leader is : {} ", leader);
        }
    };
    
    protected final ClassMatchedHandler<KVOperation, Message> bebHandler = new ClassMatchedHandler<KVOperation, Message>() {
        @Override
        public void handle(KVOperation event, Message ctx) {
        	LOG.info("Beb delivered operation from {}",ctx.getSource());
        	if(event.request.equals("put")){
        		map.put(event.key, event.value);
        		LOG.info("Local map updated (put) with key:value - {},{}", event.key, event.value);
        	}else if(event.request.equals("cas")){
        		map.put(event.key, event.value);
        		LOG.info("Local map updated (cas) with key:value - {},{}", event.key, event.value);
        	}
        }
    };

	{
		map = new HashMap<Integer, Integer>();

		try {
			range_start = config().getValue("id2203.project.keyRange-start", Integer.class);
			range_end = config().getValue("id2203.project.keyRange-end", Integer.class);
		} catch (ClassCastException e) {
			LOG.info("No key range_end found...");
		} catch (NullPointerException e) {
			LOG.info("No key range_start found...");
		}
		for (int i = range_start; i <= range_end; i++) {
			map.put(i, i * 1000);
		}
		
		subscribe(opHandler, net);
		subscribe(bebHandler, net);
		subscribe(repHandler, rep);
	    subscribe(ldHandler, ld);
	}
	
	
}
