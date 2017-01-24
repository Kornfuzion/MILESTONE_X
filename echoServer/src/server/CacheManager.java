package server;

import java.util.ArrayList;
import java.util.PriorityQueue;

// LRU 1
// LFU 2
// FIFO 3

public class CacheManager {
	//macros
	private int LRU = 1;
	private int LFU = 2;
	private int FIFO = 3;
	
	//class members
	private int policy;
	private int cacheSize;
	private int currentSize;
	private long priorityNum; 
	private CacheQueue queue;
	//private PriorityQueue<Cache> lfu_queue;
	//private ArrayList<Cache> fifo_queue;
	//private ArrayList<Cache> lru_queue;
	
	//constructor
	public CacheManager(int policy, int cacheSize) {
		super();
		this.policy = policy;
		this.cacheSize = cacheSize;
		currentSize = 0;
		if(policy == LRU){
			//lru_queue()
			queue = new LruQueue(cacheSize);
		}else if(policy == LFU){
			queue = new LfuQueue(cacheSize);
		}else if(policy == FIFO){
			queue = new FifoQueue(cacheSize);
		}else{
			System.out.println("something went wrong, check the policy being added. returning without constructing");
			queue = null;
		}
	}
	
	
}
