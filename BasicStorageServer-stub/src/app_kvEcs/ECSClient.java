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

public class ECSClient {
	private TreeSet<ECSNode> hashRing;
	private TreeSet<ECSNode> availableMachines;
	private Comparator<ECSNode> comparator;	
	private int totalNumberOfMachines;
	private Process proc;

    public static String ROOT_HOST_ADDRESS = "127.0.0.1";

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
				System.out.println("Socket or key is null");			
			}
			try {
				InputStream inputStream = socket.getInputStream();
				OutputStream outputStream = socket.getOutputStream();
				// Send start message to kvServer.
				KVMessage message = new KVMessage(CommandType.START)
										.setClientType(ClientType.ECS);
				KVMessageUtils.sendMessage(message, outputStream);
				KVMessage receiveMessage = KVMessageUtils.receiveMessage(inputStream);
				if(receiveMessage == null)
					System.out.println("receivemessage is null");
				System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus() + " " + receiveMessage.getMessage());
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

    public void removeNode(String portString) {
        try {
            ECSNode removeNode = null;
            for (ECSNode node : hashRing) {
                if (node.getPort().equals(portString)) {
                    removeNode = node;
                    break;
                }    
            }

            if (removeNode == null) {
                // wtf we fucked up
                return;
            }

            String hash = removeNode.getHashedValue();
            hashRing.remove(removeNode);
            Socket removeSocket = kvServerSockets.get(hash); 
            ECSNode successor = MetadataUtils.getSuccessor(hash, hashRing, false);     

            // Stop the node to be removed
            KVMessage message = new KVMessage(CommandType.STOP)
				        .setClientType(ClientType.ECS);  
		    KVMessageUtils.sendMessage(message, removeSocket.getOutputStream());
		    KVMessage receiveMessage = KVMessageUtils.receiveMessage(removeSocket.getInputStream());
		    System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());

            // Lock successor
            Socket successorSocket = kvServerSockets.get(successor.getHashedValue());
            message = new KVMessage(CommandType.LOCK_WRITE)
				        .setClientType(ClientType.ECS);  
		    KVMessageUtils.sendMessage(message, successorSocket.getOutputStream());
		    receiveMessage = KVMessageUtils.receiveMessage(successorSocket.getInputStream());
		    System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());

            // Move data to successor
            message = new KVMessage(CommandType.MOVE_DATA)
				        .setClientType(ClientType.ECS);  
		    KVMessageUtils.sendMessage(message, removeSocket.getOutputStream());
		    receiveMessage = KVMessageUtils.receiveMessage(removeSocket.getInputStream());
		    System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());

            // Shutdown node to be removed
            message = new KVMessage(CommandType.SHUT_DOWN)
				        .setClientType(ClientType.ECS);  
		    KVMessageUtils.sendMessage(message, removeSocket.getOutputStream());
		    receiveMessage = KVMessageUtils.receiveMessage(removeSocket.getInputStream());
		    System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());

            // Unlock successor
            message = new KVMessage(CommandType.UNLOCK_WRITE)
				        .setClientType(ClientType.ECS);  
		    KVMessageUtils.sendMessage(message, successorSocket.getOutputStream());
		    receiveMessage = KVMessageUtils.receiveMessage(successorSocket.getInputStream());
		    System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());

            System.out.println("*************************************************\nUPDATE ALL SERVER METADATA\n*************************************************");

            // Update metadata of all other servers
            for (ECSNode serverNode : hashRing) {
                Socket sock = kvServerSockets.get(serverNode.getHashedValue());
                message = new KVMessage(CommandType.UPDATE_METADATA)
                                                .setMetadata(hashRing)
									            .setClientType(ClientType.ECS);  
		        KVMessageUtils.sendMessage(message, sock.getOutputStream());
		        receiveMessage = KVMessageUtils.receiveMessage(sock.getInputStream());
		        System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addNode(String portString, String cacheSizeString, String cachePolicyString) {
        try {
            int port = Integer.parseInt(portString);
            int cacheSize = Integer.parseInt(cacheSizeString);
            CachePolicy cachePolicy = CachePolicy.parseString(cachePolicyString);

            String script = "script.sh";
		    Process p = Runtime.getRuntime().exec("hostname -f");
		    p.waitFor();
		    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    String hostname = br.readLine();
		    if (hostname == null) {
			    System.out.println("Could not get hostname of machine. Please restart");
			    System.exit(0);
		    }	

		    ECSNode node = new ECSNode(Integer.toString(port), ROOT_HOST_ADDRESS);
            ECSNode successor = MetadataUtils.getSuccessor(node.getHashedValue(), hashRing, false);
            Socket successorSocket = kvServerSockets.get(successor.getHashedValue());
		    p = Runtime.getRuntime().exec(script + " " + hostname + " " + port);
		    p.waitFor();
		    hashRing.add(node);

		    try {
			    Thread.sleep(5000);
		    } catch (InterruptedException e) {
			    System.out.println(e);
		    }

            System.out.println("*************************************************\nLAUNCH AND INIT NEW SERVER\n*************************************************");

            Socket kvServerSocket = new Socket(node.getIP(), Integer.parseInt(node.getPort()));

		    // VICTOR CHANGE THIS
		    KVMessage message = new KVMessage(CommandType.INIT)
										    .setMetadata(hashRing)
										    .setCacheSize(cacheSize)
										    .setCachePolicy(cachePolicy)
									        .setClientType(ClientType.ECS);  
		    KVMessageUtils.sendMessage(message, kvServerSocket.getOutputStream());
		    KVMessage receiveMessage = KVMessageUtils.receiveMessage(kvServerSocket.getInputStream());
		    System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());
		    kvServerSockets.put(node.getHashedValue(), kvServerSocket);

            System.out.println("*************************************************\nSTART NEW SERVER\n*************************************************");

            // Start the new server
            message = new KVMessage(CommandType.START)
								        .setClientType(ClientType.ECS);
		    KVMessageUtils.sendMessage(message, kvServerSocket.getOutputStream());
		    receiveMessage = KVMessageUtils.receiveMessage(kvServerSocket.getInputStream());
            System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());

            System.out.println("*************************************************\nLOCK WRITE SUCCESSOR\n*************************************************");

            // Lock successor
            message = new KVMessage(CommandType.LOCK_WRITE)
				        .setClientType(ClientType.ECS);  
		    KVMessageUtils.sendMessage(message, successorSocket.getOutputStream());
		    receiveMessage = KVMessageUtils.receiveMessage(successorSocket.getInputStream());
		    System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());

            System.out.println("*************************************************\nMOVE SUCCESSOR DATA\n*************************************************");

            // Move data
            message = new KVMessage(CommandType.MOVE_DATA)
				        .setClientType(ClientType.ECS);  
		    KVMessageUtils.sendMessage(message, successorSocket.getOutputStream());
		    receiveMessage = KVMessageUtils.receiveMessage(successorSocket.getInputStream());
		    System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());
            
            System.out.println("*************************************************\nUNLOCK WRITE SUCCESSOR \n*************************************************");

            // Unlock successor
            message = new KVMessage(CommandType.UNLOCK_WRITE)
				        .setClientType(ClientType.ECS);  
		    KVMessageUtils.sendMessage(message, successorSocket.getOutputStream());
		    receiveMessage = KVMessageUtils.receiveMessage(successorSocket.getInputStream());
		    System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());

            System.out.println("*************************************************\nUPDATE ALL SERVER METADATA\n*************************************************");

            // Update metadata of all other servers
            for (ECSNode serverNode : hashRing) {
                Socket sock = kvServerSockets.get(serverNode.getHashedValue());
                message = new KVMessage(CommandType.UPDATE_METADATA)
                                                .setMetadata(hashRing)
									            .setClientType(ClientType.ECS);  
		        KVMessageUtils.sendMessage(message, sock.getOutputStream());
		        receiveMessage = KVMessageUtils.receiveMessage(sock.getInputStream());
		        System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
     }

	public void initService(int numberOfNodes, int cacheSize, String replacementStrategy){
		runConfig("testECSClient.config");
		try {
			String script = "script.sh";
			Process p = Runtime.getRuntime().exec("hostname -f");
			p.waitFor();
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String hostname = br.readLine();
			if (hostname == null) {
				System.out.println("Could not get hostname of machine. Please restart");
				System.exit(0);
			}	

			// Starting up the KVServers.
			for (int i = 0; i < numberOfNodes; i++) {
				ECSNode node = availableMachines.pollFirst();
				p = Runtime.getRuntime().exec(script + " " + hostname + " " + node.getPort());
				p.waitFor();
				hashRing.add(node);
			}
			
			// Sleeping for 5s before trying to connect to the KVServers.
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				System.out.println(e);
			}

			// Connecting and initializing the KVServers
			for (ECSNode node : hashRing) {
				Socket kvServerSocket = new Socket(node.getIP(), Integer.parseInt(node.getPort()));
				// VICTOR CHANGE THIS
				KVMessage ringMessage = new KVMessage(CommandType.INIT)
												.setMetadata(hashRing)
												.setCacheSize(cacheSize)
												.setCachePolicy(CachePolicy.parseString(replacementStrategy))
											    .setClientType(ClientType.ECS);  
				KVMessageUtils.sendMessage(ringMessage, kvServerSocket.getOutputStream());
				KVMessage receiveMessage = KVMessageUtils.receiveMessage(kvServerSocket.getInputStream());
				System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());
				kvServerSockets.put(node.getHashedValue(), kvServerSocket);
	
			}
		}catch (IOException e) {
		 	e.printStackTrace();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		} catch (Exception ge) { 
			ge.printStackTrace();
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
			ioe.printStackTrace();	
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

	public int getTotalNumberOfMachines(){
		return totalNumberOfMachines;
	}

    public TreeSet<ECSNode> getAvailableMachines() {
        return availableMachines;
    }
	
	public TreeSet<ECSNode> getHashRing(){
		return hashRing;
	}
 
	public int getHashRingSize(){
		return hashRing.size();	
	}

	public int getAvailableMachinesSize(){
		return availableMachines.size(); 
	}

	public void runConfig(String fileName){
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
					addToAvailableMachines(splited[0], splited[1]);
				}
			}
			fileReader.close();
		} catch (IOException e){
			e.printStackTrace();		
		}	
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
		
		client.initService(5, 1, "none");
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
