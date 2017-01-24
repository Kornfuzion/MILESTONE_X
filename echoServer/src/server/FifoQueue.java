package ece419StorageManager;

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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getValue(String Key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean cachePush(String Key, String Value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean cachePop(String Key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean setValue(String Key, String newValue) {
		// TODO Auto-generated method stub
		return false;
	}

	

}
