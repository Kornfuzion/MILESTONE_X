package app_kvServer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

public class LruQueue implements CacheQueue{
	
	//for this class, we do not use the timesUsed member in Cache
	private ArrayList<Cache> lru_queue;
	private int max_size;
	
	//constructor
	public LruQueue(int size) {
		super();
		lru_queue = new ArrayList<Cache>();
		max_size = size;
	}

	@Override
	public boolean find(String Key) {
		for(Cache c : lru_queue){
			if(Key.equals(c.getKey())){
				return true;
			}
		}
		return false;
	}

	//returns the value for the key,value pair, re-add the object to the end of the list
	@Override
	public String getValue(String Key) {
		for(Cache c : lru_queue){
			if(Key.equals(c.getKey())){
				//create a new cache object, remove the current one "C" and add "D" to the end of the list
				Cache d = new Cache(0,c.getKey(),c.getValue());
				lru_queue.remove(c);
				lru_queue.add(d);
				return c.getValue();
			}
		}
		return null;
	}

	//adds a new object to the cache, if cache is full, evict 1 object based on lfu policy (times used variable) then add
	@Override
	public boolean cachePush(String Key, String Value) {
		if(lru_queue.size() < max_size){ //still have space in cache, do a simple add
			Cache c = new Cache(0, Key, Value);
			if(lru_queue.add(c)){
				//do some logging here
				System.out.println("Successfully added Key:" + Key + " Value: " + Value + "Into LRU Queue");
				return true;
			}
		}
		else{
			//delete the lru object,
			while(lru_queue.size() >= max_size){ //should only happen once..
				lru_queue.remove(0); //removes the first element in the list
			}
			Cache c = new Cache(0, Key, Value);
			if(lru_queue.add(c)){
				//do some logging here
				System.out.println("Successfully added Key:" + Key + " Value: " + Value + "Into LRU Queue");
				return true;
			}
		}
		return false;
	}

	//delete the object of key, if its not there return false
	@Override
	public boolean cachePop(String Key) {
		for(Cache c : lru_queue){
			if(Key.equals(c.getKey())){
				lru_queue.remove(c);
			}
		}
		return false;
	}

	//set a new value to cache obj with key,
	@Override
	public boolean setValue(String Key, String newValue) {
		for(Cache c : lru_queue){
			if(Key.equals(c.getKey())){
				//create a new cache object, remove the current one "C" and add "D" to the end of the list
				Cache d = new Cache(0,c.getKey(),newValue);
				lru_queue.remove(c);
				lru_queue.add(d);
				return true;
			}
		}
		return false;
	}
	
	
}
