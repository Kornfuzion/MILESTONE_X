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
		        sendReceiveMessage(CommandType.START, socket);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
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
				// Send start message to kvServer.
                sendReceiveMessage(CommandType.STOP, socket);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			// Wait for ack message from kvServer.
		}
		return true;
	}

    public boolean removeNode(int serverIndex) {
        if (serverIndex >= hashRing.size()) {
            System.out.println("Index out of range");
            return false;
        }
        ECSNode removeNode = null;
        int ind = 0;
        for (ECSNode node: hashRing) {
            if (ind == serverIndex) {
                removeNode = node;
                break;
            } 
            ind++;
        }
        if (removeNode == null) {
            // Should never get here.
            return false;
        }
        try {
            String hash = removeNode.getHashedValue();
            Socket removeSocket = kvServerSockets.get(hash); 
            hashRing.remove(removeNode);
            ECSNode successor = MetadataUtils.getSuccessor(hash, hashRing, false);
            Socket successorSocket = kvServerSockets.get(successor.getHashedValue());                  
            
            // Lock the node to be removed.
            sendReceiveMessage(CommandType.LOCK_WRITE, removeSocket);

            // Lock the successor.
            sendReceiveMessage(CommandType.LOCK_WRITE, successorSocket);

            // Send metadata update to successor.
            setMetadata(CommandType.UPDATE_METADATA, hashRing, 0, CachePolicy.FIFO, successorSocket);

            // Notify the node that it should stop receiving any connections, including reads.
            sendReceiveMessage(CommandType.STOP, removeSocket);
            
            // Shutdown the node to be removed (no need to worry about releasing the write lock)
            // Don't need to wait for a respnse.
            sendMessage(CommandType.SHUT_DOWN, removeSocket);

            // Unlock the successor
            sendReceiveMessage(CommandType.UNLOCK_WRITE, successorSocket);

            // Update all server metadata.
            updateAllMetadata();

            // Add node back to available machines.
            availableMachines.add(removeNode); 
            // Remove node's socket.
            kvServerSockets.remove(removeNode.getHashedValue());
		return true;
        } catch (Exception e) {
            e.printStackTrace();
		return false;
        }
    }

    public boolean addNode(int cacheSize, String cachePolicyString) {
        // No more available machines to add.
        if (availableMachines.size() == 0) {
            System.out.println("No more available machines to add");
            return false;
        }
        try {
			String script = "script.sh";
			Process p = Runtime.getRuntime().exec("hostname -f");
			p.waitFor();
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String hostname = br.readLine();

			if (hostname == null) {
				System.out.println("Could not get hostname of machine. Please restart");
				return false;
			}	 

            ECSNode node = availableMachines.pollFirst();
            p = Runtime.getRuntime().exec(script + " " + hostname + " " + node.getPort());
			p.waitFor();
		    hashRing.add(node);

		    try {
			    Thread.sleep(5000);
		    } catch (InterruptedException e) {
			    System.out.println(e);
		    }

            Socket kvServerSocket = new Socket(node.getIP(), Integer.parseInt(node.getPort()));
			kvServerSockets.put(node.getHashedValue(), kvServerSocket); 
            CachePolicy cachePolicy = CachePolicy.parseString(cachePolicyString);
            setMetadata(CommandType.INIT, hashRing, cacheSize, cachePolicy, kvServerSocket);
	
		    hashRing.add(node);

            ECSNode successor = MetadataUtils.getSuccessor(node.getHashedValue(), hashRing, false);
            Socket successorSocket = kvServerSockets.get(successor.getHashedValue());
		    
            // INIT NEW SERVER
            setMetadata(CommandType.INIT, hashRing, cacheSize, cachePolicy, kvServerSocket);

            // Start the new server
            sendReceiveMessage(CommandType.START, kvServerSocket);

            // Lock successor
            sendReceiveMessage(CommandType.LOCK_WRITE, successorSocket);

            // Move data
            sendReceiveMessage(CommandType.MOVE_DATA, successorSocket);

            // Update successor's metadata while under write lock
            sendReceiveMessage(CommandType.LOCK_WRITE_UPDATE_METADATA, successorSocket);

            // Update all server metadata
            updateAllMetadata();

            // Unlock successor
            sendReceiveMessage(CommandType.UNLOCK_WRITE, successorSocket);
		return true;
        } catch (Exception e) {
            e.printStackTrace();
		return false;
        }
     }

	public void initKVService(int numberOfNodes, int cacheSize, String replacementStrategy){
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
				kvServerSockets.put(node.getHashedValue(), kvServerSocket);
                setMetadata(CommandType.INIT, 
                            hashRing, 
                            cacheSize, 
                            CachePolicy.parseString(replacementStrategy), 
                    kvServerSocket);
			}
		}catch (IOException e) {
		 	e.printStackTrace();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		} catch (Exception ge) { 
			ge.printStackTrace();
		}
	}

    public void setMetadata(CommandType commandType, 
                            TreeSet<ECSNode> hashRing, 
                            int cacheSize, 
                            CachePolicy cachePolicy, 
                            Socket socket) throws Exception{
		KVMessage ringMessage = new KVMessage(commandType)
										.setMetadata(hashRing)
										.setCacheSize(cacheSize)
										.setCachePolicy(cachePolicy)
										.setClientType(ClientType.ECS);  
		KVMessageUtils.sendMessage(ringMessage, socket.getOutputStream());
		KVMessage receiveMessage = KVMessageUtils.receiveMessage(socket.getInputStream());
		System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());
    }

    public void sendReceiveMessage(CommandType commandType, Socket socket) throws Exception{
        KVMessage message = new KVMessage(commandType)
				            .setClientType(ClientType.ECS);  
		KVMessageUtils.sendMessage(message, socket.getOutputStream());
	    KVMessage receiveMessage = KVMessageUtils.receiveMessage(socket.getInputStream());
	    System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());
    }

    public void sendMessage(CommandType commandType, Socket socket) throws Exception{
        KVMessage message = new KVMessage(commandType)
				            .setClientType(ClientType.ECS);  
		KVMessageUtils.sendMessage(message, socket.getOutputStream());
    }


    public void updateAllMetadata() throws Exception {
        // Update metadata of all other servers
        for (ECSNode serverNode : hashRing) {
            Socket socket = kvServerSockets.get(serverNode.getHashedValue());
            setMetadata(CommandType.UPDATE_METADATA, hashRing, 0, CachePolicy.FIFO, socket);
        }
    }

	public boolean shutDown(){
        try {
            for (Map.Entry<String, Socket> entry: kvServerSockets.entrySet()) {
                Socket socket = entry.getValue();
                sendReceiveMessage(CommandType.LOCK_WRITE, socket);
                sendReceiveMessage(CommandType.STOP, socket);
                sendMessage(CommandType.SHUT_DOWN, socket);
            }
        } catch (Exception e) {
            System.out.println("Could not shut down all servers");
		return false;
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
				if (splited.length == 2) {
					addToAvailableMachines(splited[0], splited[1]);
				}
			}
			fileReader.close();
		} catch (IOException e){
			e.printStackTrace();		
		}	
	}

	/*public static void main(String[] args){
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
		
		
		return;			
	}*/
	

	
	
}
