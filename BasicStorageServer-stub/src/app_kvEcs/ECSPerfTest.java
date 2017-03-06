package app_kvEcs; 

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import java.lang.InterruptedException;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.net.Socket;
import java.net.UnknownHostException;
import common.messages.*;
import common.*;
import cache.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;

public class ECSPerfTest {
	public static void main(String[] args){
		int numClients = Integer.parseInt(args[0]);
		int middle = Integer.parseInt(args[1]);
		System.out.println("Starting PerfTest Main");
		ECSClient client = new ECSClient();
		client.initKVService(5, 100, "FIFO", "testECSClient.config");
		client.start();
		Map map = new HashMap<String, String>();
		try{
			System.out.println("Starting parsing file");
			FileInputStream stream = new FileInputStream("output.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line = reader.readLine();
			while(line != null){
				line = line.replace("\"", "");
				String[] arg = line.split(",");
				
				//System.out.println(arg.length);
				if(arg.length == 2){
					//System.out.println(arg[0] + " " + arg[1]);
					map.put(arg[0], arg[1]);
				}
				line = reader.readLine();
			}
			ArrayList<Thread> threadList = new ArrayList<Thread>();
			long startTime = System.currentTimeMillis();
			for(int i = 0; i < numClients ; i++){
				Thread t = new Thread(new ECSPerfRunnable(map, middle));
				threadList.add(t);
				t.start();		
			}
			for(Thread t : threadList){
				try{
				t.join();
				} catch (InterruptedException e){
					System.out.println("hit interrupt exception");				
				}			
			}
			long endTime = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			System.out.println("Time " + totalTime);
		} catch (FileNotFoundException e) {
			System.out.println("file not found in perf test");
		} catch (IOException e){
			System.out.println("ioexception found in perftest");
		}
		
	}
}
