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

    class Heartbeater implements Runnable {

        ECSClient client;
        ECSNode server;
        Socket connection;
        static final int heartbeatInterval = 5000;

        public Heartbeater(ECSClient client, ECSNode server) {
            this.client = client;
            this.server = server;
            this.connection = client.getHeartbeatSocket(this.server.getHashedValue());
        }

        public void run() {
            // send heartbeats every X seconds
            while (true) {
                try {
                    KVMessageUtils.sendReceiveMessage(CommandType.HEARTBEAT, this.connection, false);
                    Thread.sleep(this.heartbeatInterval);
                } catch (Exception e) {
                    break;
                }
            }
            // handle failure

			this.server.setNodeDead();
            client.removeHeartbeatSocket(server.getHashedValue());
            client.handleFailure(this.server);
        }
    }

    // Holds the communication socket to every KVServer for ECS commands
    private Map<String, Socket> kvServerSockets;
    private Map<String, Socket> heartbeatSockets;

    public ECSClient(){
        this.comparator = new hashRingComparator();         
        this.hashRing = new TreeSet<ECSNode>(comparator);
        this.availableMachines = new TreeSet<ECSNode>(comparator);
        this.kvServerSockets = new LinkedHashMap<String, Socket>();
        this.heartbeatSockets = new LinkedHashMap<String, Socket>();
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
                return false;           
            }
            try {
                KVMessageUtils.sendReceiveMessage(CommandType.START, socket);
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
                return false;           
            }
            try {
                // Send start message to kvServer.
                KVMessageUtils.sendReceiveMessage(CommandType.STOP, socket);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            // Wait for ack message from kvServer.
        }
        return true;
    }

    public void handleFailure(ECSNode deadServer) {
        // TODO: VICTOR KO SMD BISH
        System.out.println("HANDLING FAILURE FOR DEAD SERVER "+ deadServer.getPort());
		//remove from active ring
		int ind = 0;		
		for(ECSNode node : hashRing){
			if(node.getPort() == deadServer.getPort()){
				break;			
			}
			ind++;		
		}
		//found index of dead server, call remove Node
		removeNode(ind);
		addNode(500, "lru");
		
		
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
            
			// Removing the last node (no successor).
			if (successor == null) {
				if(removeNode.getLiveness() == true){
					KVMessageUtils.sendReceiveMessage(CommandType.LOCK_WRITE, removeSocket);
            		KVMessageUtils.sendReceiveMessage(CommandType.STOP, removeSocket);
            		sendMessage(CommandType.SHUT_DOWN, removeSocket);
				}
				return true;
			}        

            Socket successorSocket = kvServerSockets.get(successor.getHashedValue());                  
            
            // Lock the node to be removed.
			if(removeNode.getLiveness() == true){
            	KVMessageUtils.sendReceiveMessage(CommandType.LOCK_WRITE, removeSocket);
			}

            // Lock the successor.
            KVMessageUtils.sendReceiveMessage(CommandType.LOCK_WRITE, successorSocket);

            // Send metadata update to successor.
            setMetadata(CommandType.UPDATE_METADATA, hashRing, 0, CachePolicy.FIFO, successorSocket);

            // Notify the node that it should stop receiving any connections, including reads.
			if(removeNode.getLiveness() == true){
            	KVMessageUtils.sendReceiveMessage(CommandType.STOP, removeSocket);
			}
            
            // Shutdown the node to be removed (no need to worry about releasing the write lock)
            // Don't need to wait for a respnse.
			if(removeNode.getLiveness() == true){
            	sendMessage(CommandType.SHUT_DOWN, removeSocket);
			}

            // Unlock the successor
            KVMessageUtils.sendReceiveMessage(CommandType.UNLOCK_WRITE, successorSocket);

            // Update all server metadata.
            updateAllMetadata();

            // Add node back to available machines.
			removeNode.setNodeAlive();
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
            String script = "./script.sh";
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
            Socket heartbeatSocket = new Socket(node.getIP(), Integer.parseInt(node.getPort()));
            kvServerSockets.put(node.getHashedValue(), kvServerSocket); 
            heartbeatSockets.put(node.getHashedValue(), kvServerSocket); 
            CachePolicy cachePolicy = CachePolicy.parseString(cachePolicyString);
    
            hashRing.add(node);

            ECSNode successor = MetadataUtils.getSuccessor(node.getHashedValue(), hashRing, false);
            Socket successorSocket = kvServerSockets.get(successor.getHashedValue());
            
            // INIT NEW SERVER
            setMetadata(CommandType.INIT, hashRing, cacheSize, cachePolicy, kvServerSocket);

            // Start the new server
            KVMessageUtils.sendReceiveMessage(CommandType.START, kvServerSocket);

            // Lock successor
            KVMessageUtils.sendReceiveMessage(CommandType.LOCK_WRITE, successorSocket);

            // Move data
            KVMessageUtils.sendReceiveMessage(CommandType.MOVE_DATA, successorSocket);

            // Update successor's metadata while under write lock
            KVMessageUtils.sendReceiveMessage(CommandType.LOCK_WRITE_UPDATE_METADATA, successorSocket);

            // Update all server metadata
            updateAllMetadata();

            // Unlock successor
            KVMessageUtils.sendReceiveMessage(CommandType.UNLOCK_WRITE, successorSocket);

            // Start up a heartbeater for this server. If it dies,
            // the thread will execute the appropriate failure handling on callback
            new Thread(new Heartbeater(this, node)).start();
        return true;
        } catch (Exception e) {
            e.printStackTrace();
        return false;
        }
     }

    public int initKVService(int numberOfNodes, int cacheSize, String replacementStrategy, String configFile){
        runConfig(configFile);
        try {
            String script = "./script.sh";
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
                return 1;
            }

            // Connecting and initializing the KVServers
            for (ECSNode node : hashRing) {
                Socket kvServerSocket = new Socket(node.getIP(), Integer.parseInt(node.getPort()));
                Socket heartbeatSocket = new Socket(node.getIP(), Integer.parseInt(node.getPort()));
                kvServerSockets.put(node.getHashedValue(), kvServerSocket);
                heartbeatSockets.put(node.getHashedValue(), heartbeatSocket);
                
                try{
                    setMetadata(CommandType.INIT, 
                                hashRing, 
                                cacheSize, 
                                CachePolicy.parseString(replacementStrategy), 
                        kvServerSocket);

                    // Start up a heartbeater for this server, if it dies,
                    // the thread will execute the appropriate failure handling on callback
                    new Thread(new Heartbeater(this, node)).start();
                } catch (Exception ge) {
                    System.out.println("Unable to send metadata to server at port " 
                                        + node.getPort() + ". It is most likely being used.");
                    return 1;
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
            return 1;
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            return 1;
        }

        // Successfully initialized all servers.
        return 0;
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
                KVMessageUtils.sendReceiveMessage(CommandType.LOCK_WRITE, socket);
                KVMessageUtils.sendReceiveMessage(CommandType.STOP, socket);
                sendMessage(CommandType.SHUT_DOWN, socket);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not shut down all servers");
        return false;
        }
        return true;
    }

    // Err...we never use this function. Remove it or what?
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

    public Socket getServerSocket(String hash) {
        return kvServerSockets.get(hash);
    }

    public Socket getHeartbeatSocket(String hash) {
        return heartbeatSockets.get(hash);
    }

    public void removeHeartbeatSocket(String hash) {
        heartbeatSockets.remove(hash);
    }

    public void sendMessage(CommandType commandType, Socket socket) throws Exception {
        KVMessage message = new KVMessage(commandType)
                            .setClientType(ClientType.ECS);  
        KVMessageUtils.sendMessage(message, socket.getOutputStream());
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
