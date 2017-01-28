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
	
	public String get(String key){
		String s = queue.getValue(key);
		if(s != null){
			return s;
		}
		else{
			return null;
		}
	}
	
	public boolean update(String key, String value){
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
	
	public boolean delete( String key){
		if(queue.cachePop(key)){
			return true;
		}
		else {
			return false;
		}
	}
	
	public int getSize(){
		return queue.getSize();
	}
	
	public void printQueue(){
		queue.printQueue();
	}
	
}
