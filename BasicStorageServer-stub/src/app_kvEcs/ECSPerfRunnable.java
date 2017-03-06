package app_kvEcs; 
import common.messages.*;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
import java.net.Socket;
import cache.*;
import datastore.*;
import app_kvClient.client.ClientSocketListener.SocketStatus;
import client.KVStore;
import app_kvClient.ui.*;
import app_kvClient.client.*;

public class ECSPerfRunnable implements Runnable{
	Map<String,String> map;
	int mid;
	Application app;
	
	ECSPerfRunnable(Map map, int middle){
		this.map = map;
		this.mid = middle;
	}

	public void run(){
		// create socket, connect and then send shit over
		try {
			app = new Application(true);
			
		} catch (Exception e){
		
		}		
		int i = 0;
	
        int version = 0;
		for(String key : this.map.keySet()){
			String val = this.map.get(key);
			//System.out.print(key + " and " + val);
			//do stuff here
			
			if(i <= mid){ //400 250 100
				try{
				//System.out.println("put");
				KVMessage message = app.put(key,val);
				//System.out.println("PUT key " + message.getKey() + " value " + message.getValue() + " status " + message.getStatus() + "\n" + message.getMessage());
				} catch (Exception e){
					e.printStackTrace();
					break;
				}
			}
			else{
				try{
				app.get(key);
				//System.out.println("get");
				} catch (Exception e){
					e.printStackTrace();
					break;
				}
			}
			i++;
			if(i == mid*2) 
				break;		
		}
		System.out.println("Done running Thread");
	}
}

