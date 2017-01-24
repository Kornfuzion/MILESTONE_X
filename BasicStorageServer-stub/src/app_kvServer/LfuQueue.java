package app_kvServer;

import java.util.Comparator;
import java.util.PriorityQueue;

public class LfuQueue implements CacheQueue{

	private PriorityQueue<Cache> lfu_queue;
	private int max_size;
	
	//constructor
	public LfuQueue(int size) {
		super();
                // TODO(victor): The following line is in Java 8 syntax. Need to switch it to Java 7.
		//this.lfu_queue = new PriorityQueue<Cache>(Comparator.comparing(Cache::getTimesUsed));
		this.max_size = size;
	}

	//search if the key is in the cache
	@Override
	public boolean find(String Key) {
		for(Cache c : lfu_queue){
			if(Key.equals(c.getKey())){
				return true;
			}
		}
		return false; 
	}

	//returns the value for the key,value pair, increment the times used
	@Override
	public String getValue(String Key) {
		for(Cache c : lfu_queue){
			if(Key.equals(c.getKey())){
				c.setTimesUsed(c.getTimesUsed() + 1); //increment the times used
				return c.getValue();
			}
		}
		return null;
	}

	//adds a new object to the cache, if cache is full, evict 1 object based on lfu policy (times used variable) then add
	@Override
	public boolean cachePush(String Key, String Value) {
		if(lfu_queue.size() < max_size){ //still have space in cache, do a simple add
			Cache c = new Cache(0, Key, Value);
			if(lfu_queue.add(c)){
				//do some logging here
				System.out.println("Successfully added Key:" + Key + " Value: " + Value + "Into LRU Queue");
				return true;
			}
		}
		else{
			//delete the smallest priority cache node, then add the new cache object
			while(lfu_queue.size() >= max_size){ //doing a while loop, not sure if you ever go past max size... shouldnt though
				lfu_queue.poll(); //deletes based the cache node with smallest times used
			}
			Cache c = new Cache(0, Key, Value);
			if(lfu_queue.add(c)){
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
		for(Cache c : lfu_queue){
			if(Key.equals(c.getKey())){
				lfu_queue.remove(c);
				return true;
			}
		}
		return false;
	}

	//set a new value to cache obj with key, incr the times used variable
	@Override
	public boolean setValue(String Key, String newValue) {
		for(Cache c : lfu_queue){
			if(Key.equals(c.getKey())){
				c.setValue(newValue);
				c.setTimesUsed(c.getTimesUsed() + 1);
				return true;
			}
		}
		return false;
	}

}
