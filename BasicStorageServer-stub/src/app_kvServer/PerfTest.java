package app_kvServer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import cache.*;
import datastore.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;



public class PerfTest {
	public static void main(String args[]){
		System.out.println("Starting Perftest Main");
		final int numThreads = 2;
		//String storagePath = System.getProperty("user.dir") + File.separator + "storage";
		//StorageManager storageManager = new StorageManager("LRU", 100, storagePath);
		KVServer kvs = new KVServer(2017, 50, "LRU");
		StorageManager storageManager = kvs.getStorageManager();
		Map map = new HashMap<String, String>();

		try{
			//String storagePath = System.getProperty("user.dir") + File.separator + "storage.";
			FileInputStream stream = new FileInputStream("perfData.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line = reader.readLine();
			while(line != null){
				String[] arg = line.split(" ");
				System.out.println(arg.length);
				if(arg.length == 2){
					System.out.println(arg[0] + " " + arg[1]);
					map.put(arg[0], arg[1]);
				}
				line = reader.readLine();
			}
			for(int i = 0; i < numThreads ; i++){
				new Thread(new PerfRunnable(map, storageManager)).start();		
			}
		} catch (FileNotFoundException e) {
			System.out.println("file not found in perf test");
		} catch (IOException e){
			System.out.println("ioexception found in perftest");
		}

	}
}
