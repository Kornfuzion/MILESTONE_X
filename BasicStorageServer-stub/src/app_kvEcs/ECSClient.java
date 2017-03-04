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
import java.net.Socket;
import java.net.UnknownHostException;
import common.messages.*;



public class ECSClient {
	private TreeSet<ECSNode> hashRing;
	private TreeSet<ECSNode> availableMachines;
	private Comparator<ECSNode> comparator;	
	private int totalNumberOfMachines;
	private Process proc;

	// Holds the communication socket to every KVServer
	private Map<String, Socket> kvServerSockets;

	public ECSClient(){
		this.comparator = new hashRingComparator();			
		this.hashRing = new TreeSet<ECSNode>(comparator);
		this.availableMachines = new TreeSet<ECSNode>(comparator);
		this.kvServerSockets = new LinkedHashMap<String, Socket>();
		totalNumberOfMachines = 0;
	}
	
	public boolean start(){
		//activate all the runningservers to start the service available
		//send message to these servers to go for it.
		for (Map.Entry<String, Socket> entry: kvServerSockets.entrySet()) {
			String key = entry.getKey();
			Socket socket = entry.getValue();
			if(socket == null || key == null){
				System.out.println("shits null");			
			}
			try {
				InputStream inputStream = socket.getInputStream();
				OutputStream outputStream = socket.getOutputStream();
				// Send start message to kvServer.
				KVMessage message = new KVMessage(CommandType.START)
										.setClientType(ClientType.ECS);
				System.out.println(message.getCommand());
				KVMessageUtils.sendMessage(message, outputStream);
				KVMessage receiveMessage = KVMessageUtils.receiveMessage(inputStream);
				if(receiveMessage == null)
					System.out.println("receivemessage is null");
				System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());
			} catch (Exception e) {
				e.printStackTrace();
			}

			
			// Wait for ack message from kvServer.
		}
		return true;
	}

	public boolean stop(){
		//stops serving requests to clients, but the processes are still alive???
		//send message to these servers to stop serving requests
		for (Map.Entry<String, Socket> entry: kvServerSockets.entrySet()) {
			String key = entry.getKey();
			Socket socket = entry.getValue();
			if(socket == null || key == null){
				System.out.println("shits null");			
			}
			try {
				InputStream inputStream = socket.getInputStream();
				OutputStream outputStream = socket.getOutputStream();
				// Send start message to kvServer.
				KVMessage message = new KVMessage(CommandType.STOP)
										.setClientType(ClientType.ECS);
				System.out.println(message.getCommand());
				KVMessageUtils.sendMessage(message, outputStream);
				KVMessage receiveMessage = KVMessageUtils.receiveMessage(inputStream);
				if(receiveMessage == null)
					System.out.println("receivemessage is null");
				System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());
			} catch (Exception e) {
				e.printStackTrace();
			}

			
			// Wait for ack message from kvServer.
		}
		return true;
	}

	public void initService(int numberOfNodes, int cacheSize, String replacementStrategy){

		try {
			String size = Integer.toString(cacheSize);
			System.out.println("running script");
		
			// Get the hostname of the ug machine
			Process p = Runtime.getRuntime().exec("hostname -f");
			p.waitFor();
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String l = br.readLine();
			if (l == null) {
				System.out.println("Could not get hostname of machine. Please restart");
				System.exit(0);
			}			

			/*String script = "script.sh";
			Runtime run = Runtime.getRuntime();
		  	this.proc = run.exec(script + " " +l);*/
		} catch (IOException e) {
		 	e.printStackTrace();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		System.out.println("finished script");

		//for actual init service
			
		for(int i=0; i< numberOfNodes ; i++){
			ECSNode node = availableMachines.pollFirst();
			Socket kvServerSocket = null;
			try {
				int portNumber = 0;
				kvServerSocket = new Socket(node.getIP(), Integer.parseInt(node.getPort()));
			} catch (IOException ioe) {
				System.out.println(ioe);
			}
			kvServerSockets.put(node.getHashedValue(), kvServerSocket);
			String port = node.getPort();
			String IP = node.getIP();
			/*System.out.println("running script");
			String script = "script.sh ug168.eecg.utoronto.ca" + size + " " + replacementStrategy;
			Runtime run = Runtime.getRuntime();
			try {
			  this.proc = run.exec(script);
			} catch (IOException e) {
			  e.printStackTrace();
			}
			System.out.println("finished script");
			*/
			hashRing.add(node);
		}

	}

	public boolean shutDown(){
		//stops all server instances and exits the remote processes
		for (Map.Entry<String, Socket> entry: kvServerSockets.entrySet()) {
			String key = entry.getKey();			
			Socket socket = entry.getValue();
			if(socket == null || key == null){
				System.out.println("shits null");			
			}
			try {
				InputStream inputStream = socket.getInputStream();
				OutputStream outputStream = socket.getOutputStream();
				// Send start message to kvServer.
				KVMessage message = new KVMessage(CommandType.SHUT_DOWN)
										.setClientType(ClientType.ECS);
				System.out.println(message.getCommand());
				KVMessageUtils.sendMessage(message, outputStream);
				KVMessage receiveMessage = KVMessageUtils.receiveMessage(inputStream);
				if(receiveMessage == null)
					System.out.println("receivemessage is null");
				System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());
				//close the socket connection.
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return true;
	}

	public boolean addToHashRing(String arg1, String arg2){
		ECSNode node = availableMachines.pollFirst();
		hashRing.add(node);
		Socket kvServerSocket = null;
		try {
			int portNumber = 0;
			kvServerSocket = new Socket(node.getIP(), Integer.parseInt(node.getPort()));
		} catch (IOException ioe) {
			System.out.println(ioe);
		}
		kvServerSockets.put(node.getHashedValue(), kvServerSocket);
		totalNumberOfMachines++;
		return true;
	}
	
	public boolean addToAvailableMachines(String arg1, String arg2){
		ECSNode node = new ECSNode(arg1,arg2);
		availableMachines.add(node);
		totalNumberOfMachines++;
		return true;
	}


	public static void main(String[] args){
		ECSClient client = new ECSClient();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			System.out.println(e);
		}
		String fileName = "test.config";
		try{
			File file = new File (fileName);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			StringBuffer stringBuffer = new StringBuffer();
		
			String line;
			while((line = bufferedReader.readLine()) != null){
				String[] splited = line.split(" ");
				if(splited.length < 2){
					System.out.println("screwed up somewhere in the read file");				
				}
				else{
					client.addToAvailableMachines(splited[0], splited[1]);
				}
			}
			fileReader.close();
		} catch (IOException e){
			e.printStackTrace();		
		}
		
		client.initService(1, 1, "none");
		client.start();
		client.stop();
		//populate here lois
		
		
		//testing if it worked
		/*while(hashRing.size() > 0){
			ECSNode n = hashRing.pollFirst();
			if(n != null){
				System.out.println(n.getPort() +  " " + n.getIP() + " " + n.getHashedValue());			
			}			
		}
		*/
		// STEPS TO START UP THE KV-SERVER
		//client.initService(1,1,"none");
		// 1. Start the servers
		// 2. Wait for 5 seconds 
		return;			
	}
	

	
	
}
