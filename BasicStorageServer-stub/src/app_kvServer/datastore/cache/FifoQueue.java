package cache;

import java.util.ArrayList; 

public class FifoQueue implements CacheQueue{
	//for this class, we do not use the timesUsed member in Cache
	private ArrayList<Cache> fifo_queue;
	private int max_size;
	
	//constructor
	public FifoQueue(int max_size) {
		super();
		fifo_queue = new ArrayList<Cache>();
		this.max_size = max_size;
	}
	
	@Override
	public boolean find(String Key) {
		for(Cache c : fifo_queue){
			if(Key.equals(c.getKey())){
				return true;
			}
		}
		return false;
	}

	//if key is found, return the object's value, don't change its positioning in the queue.
	@Override
	public String getValue(String Key) {
		for(Cache c : fifo_queue){
			if(Key.equals(c.getKey())){
				return c.getValue();
			}
		}
		return null;
	}

	@Override
	public boolean cachePush(String Key, String Value) {
		if(fifo_queue.size() < max_size){ //still have space in cache, do a simple add
			Cache c = new Cache(0, Key, Value);
			if(fifo_queue.add(c)){
				//do some logging here
				System.out.println("Successfully added Key:" + Key + " Value: " + Value + "Into LRU Queue");
				return true;
			}
		}
		else{
			//delete the first added object,
			while(fifo_queue.size() >= max_size){ //should only happen once..
				fifo_queue.remove(0); //removes the first element in the list
			}
			Cache c = new Cache(0, Key, Value);
			if(fifo_queue.add(c)){
				//do some logging here
				System.out.println("Successfully added Key:" + Key + " Value: " + Value + "Into LRU Queue");
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean cachePop(String Key) {
		for(Cache c : fifo_queue){
			if(Key.equals(c.getKey())){
				fifo_queue.remove(c);
			}
		}
		return false;
	}

	@Override
	public boolean setValue(String Key, String newValue) {
		for(Cache c : fifo_queue){
			if(Key.equals(c.getKey())){
				//set the value of the cache object to the new value
				c.setValue(newValue);
				return true;
			}
		}
		return false;
	}

	

}
