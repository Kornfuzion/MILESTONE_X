package cache;

import java.util.ArrayList;
import java.util.PriorityQueue;

public class CacheManager {
	//class members
	private CachePolicy policy;
	private int cacheSize;
	private CacheQueue queue;
	
	//constructor
	public CacheManager(CachePolicy policy, int cacheSize) {
		super();
		this.policy = policy;
		this.cacheSize = cacheSize;
		switch (policy) {
            case LRU:
			    queue = new LruQueue(cacheSize);
                break;
		    case LFU:
			    queue = new LfuQueue(cacheSize);
                break;
		    case FIFO:
			    queue = new FifoQueue(cacheSize);
                break;
            default:
                queue = null;
        }
	}
	
	public synchronized String get(String key){
		String s = queue.getValue(key);
		if(s != null){
			return s;
		}
		else{
			return null;
		}
	}
	
	public synchronized boolean update(String key, String value){
		if(queue.find(key)){
			if(queue.setValue(key, value))
				return true;
			else 
				return false;
		} else {
			if(queue.cachePush(key, value)){
				return true;
			}
		}
		return false;
	}
	
	public synchronized boolean delete( String key){
		// Even if the return is null, that means key did not exist.
		queue.cachePop(key);
		return true;
		
	}
	
	public synchronized int getSize(){
		return queue.getSize();
	}
	
	public synchronized void printQueue(){
		queue.printQueue();
	}
	
}
