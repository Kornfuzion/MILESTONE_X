package cache;

import java.util.ArrayList;
import java.util.PriorityQueue;

public class CacheManager {
	//class members
	private CachePolicy policy;
	private int cacheSize;
	private CacheQueue queue;
	
	/**
	* Constructor that creates a Cachemanager given the cache policy and cache size
	* @param policy The policy type for the cache
	* @param cacheSize The amount of objects that can be stored in the cache data structure
	*/
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
	
	/**
	* get method that looks up the key in the cache.  If it is found we will return its corresponding value, and null if it was not found in cache.
	* @param key The key which is used for searching for its corresponding value in cache.
	*/
	public synchronized String get(String key){
		String s = queue.getValue(key);
		if(s != null){
			return s;
		}
		else{
			return null;
		}
	}
	
	/**
	* If it is already in cache, it updates the value to a new value, else it will add a new key,value pair to the cache.
	* key The key string corresponding to the key,value pair
	* value The value string corresponding to the key,value pair
	*/
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
	
	/**
	* If the object with the corresponding key is in the cache, it will be evicted and return a true.  If it is not in the cache, it will return a false.
	*/
	public synchronized boolean delete( String key){
		// Even if the return is null, that means key did not exist.
		queue.cachePop(key);
		return true;
		
	}

	/**
	* Returns the current cache size
	*/
	public synchronized int getSize(){
		return queue.getSize();
	}
	
	/**
	* Prints the key value pair of each cache object stored in the cache datastructure in structural order
	*/
	public synchronized void printQueue(){
		queue.printQueue();
	}
	
}
