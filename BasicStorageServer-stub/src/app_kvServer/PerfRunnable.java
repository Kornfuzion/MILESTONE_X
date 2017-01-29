package app_kvServer;

import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import cache.*;
import datastore.*;

public class PerfRunnable implements Runnable{
	Map<String,String> map;
	StorageManager storageManager;


	PerfRunnable(Map map, StorageManager storageManager){
		this.map = map;
		this.storageManager = storageManager;
	}

	public void run(){
		int i = 0;
		for(String key : this.map.keySet()){
			String val = this.map.get(key);
			//do stuff here
			if(i <= 50)
				this.storageManager.set(key, val);
			else
				this.storageManager.get(key);
			i++;		
		}
		System.out.println("Done running Thread");
	}
}
