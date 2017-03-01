package se.kth.id2203.beb;

import java.io.Serializable;

import se.sics.kompics.KompicsEvent;

public class KVOperation implements KompicsEvent, Serializable {	
		private static final long serialVersionUID = -4700507659951599123L;		
		public final String request;
		public final int key;
		public final int value;
		public KVOperation(String request, int key, int value ){
			this.request = request;
			this.key = key;
			this.value = value;			
		}	

}
