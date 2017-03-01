package se.kth.id2203.overlay;

import se.sics.kompics.KompicsEvent;

public class Pair implements KompicsEvent{
	public final int key;
	public final int value;
	public Pair(int key, int value){
		this.key = key;
		this.value = value;
	}

}
