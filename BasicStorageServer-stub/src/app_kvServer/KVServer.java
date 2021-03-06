package app_kvServer;

import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.FileNotFoundException;

import app_kvEcs.*;
import cache.*;
import common.*;
import common.messages.*;
import datastore.*;
import logger.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Represents a simple Echo Server implementation.
 */
public class KVServer extends Thread {

    private static Logger logger = Logger.getLogger(KVServer.class.getName());
    
    private int port;
    private int cacheSize;
    private CachePolicy policy;
    private ServerSocket serverSocket;
    private boolean running;
    private boolean alive;
    private StorageManager storageManager;
    private ECSNode serverNode;
    private TreeSet<ECSNode> metadata;
    private ArrayList<Socket> replicaSockets;

    private KVServerStatus status;

    /**
     * Start KV Server at given port
     * @param port given port for storage server to operate
     * @param cacheSize specifies how many key-value pairs the server is allowed 
     *           to keep in-memory
     * @param strategy specifies the cache replacement strategy in case the cache 
     *           is full and there is a GET- or PUT-request on a key that is 
     *           currently not contained in the cache. Options are "FIFO", "LRU", 
     *           and "LFU".
     */
    public KVServer(int port) {
        this.port = port;
        this.cacheSize = 0;
        this.policy = CachePolicy.FIFO;
        this.storageManager = null;
        this.running = false; 
        this.alive = false;
        this.metadata = null;
        this.serverNode = null;
        this.replicaSockets = new ArrayList<Socket>();

        this.status = new KVServerStatus(port, metadata);
    }

    // Leave this constructor as legacy, but from now on we will only be using the constructor with port number
    // The ECS client shall start each server with its respective port and initialize through messages
    public KVServer(int port, int cacheSize, String policy, TreeSet<ECSNode> metadata) {
        this.port = port;
        this.cacheSize = cacheSize;
        this.policy = CachePolicy.parseString(policy);
	this.metadata = metadata;
        this.running = true;
        this.status = new KVServerStatus(port, metadata);
        String storagePath = System.getProperty("user.dir") + File.separator + "storage";
        try {
            File storageFolder = new File(storagePath);
            if (!storageFolder.exists()) {
                if (!storageFolder.mkdir()) {
                    System.out.println("Could not create folder " + storagePath + ". Please check your permissions.");
                    System.exit(1);
                }
            } else {
                if(!storageFolder.isDirectory()) {
                    System.out.println(storagePath + " is detected as not a folder. Please either rename or delete the existing file.");
                    System.exit(1);
                }
            }
        } catch (SecurityException se) {
            System.out.println("Check write permissions for " + System.getProperty("user.dir"));
            System.exit(1);

        }
       
        try { 
            this.storageManager = new StorageManager(this.policy, cacheSize, storagePath, status);
        } catch (FileNotFoundException fnfe) {
            System.out.println("Could not find " + storagePath + ". Please try again");
            System.exit(1);
        }
    }

    /**
     * Initializes and starts the server. 
     * Loops until the the server should be closed.
     */
    public void run() {
        alive = createSocket();
        
        if(serverSocket != null) {
            while(alive){
                try {
                    Socket client = serverSocket.accept();                
                    RequestConnection connection = new RequestConnection(this, client, storageManager);
                    new Thread(connection).start();
                    
                    logger.info("Connected to " 
                            + client.getInetAddress().getHostName() 
                            +  " on port " + client.getPort());
                } catch (IOException e) {
                    logger.error("Error! " +
                            "Unable to establish connection. \n", e);
                }
            }
        }
        logger.info("Server stopped.");
    }
    
    public boolean isRunning() {
        return this.running;
    }

    public boolean alive() {
        return this.alive;
    }

    public void startServer() {
        running = true;
    }

    public void shutdownServer() {
        stopServer();
        this.alive = false;

        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.error("Error! " +
                    "Unable to close socket on port: " + port, e);
        }
        // Kill the server.
        System.exit(1);
    }

    public KVServerStatus getServerStatus() {
        return status;
    }
    public void moveData() {}

    public void blockStorageWrites() {
        status.versionWriteLock();
        status.versionWriteUnlock();
        //storageManager.blockWrites();
    }

    public void blockStorageRerouteReads(TreeSet<ECSNode> metadata) {
        status.readWriteLock();
 
        status.metadataWriteLock();
        updateMetadata(metadata);
        status.metadataWriteUnlock();

        status.readWriteUnlock();
    }

    public ArrayList<Socket> getReplicaSockets() {
        return this.replicaSockets;
    }

    public void updateReplicaConnections() {
        // Find the ECSNode corresponding to this server,
        // if we don't yet know it
        if (this.serverNode == null) {
            for (ECSNode node : metadata) {
                if (this.port == Integer.parseInt(node.getPort())) {
                    this.serverNode = node;
                    break;
                }
            }
        }

        try {
            // Close any previous replica sockets, if any
            for (Socket socket : replicaSockets) {
                socket.close();
            }
            replicaSockets.clear();

            // Create new replica sockets
            ECSNode nextNode = this.serverNode;
            for (int i = 0; i < 2; i++) {
                nextNode = this.metadata.higher(nextNode);
                if (nextNode == null) {
                    nextNode = this.metadata.first();
                }
                replicaSockets.add(new Socket(nextNode.getIP(), Integer.parseInt(nextNode.getPort())));
            }
        } catch (Exception e) {
            // Welp, we dun fukd up fam
            e.printStackTrace();
            replicaSockets.clear();
        }
    }

    public void updateMetadata(TreeSet<ECSNode> metadata) {
        this.metadata = metadata;
        status.setMetadata(metadata);
    }

    // Return a copy of the metadata, to be safe
    public TreeSet<ECSNode> getMetadata() {
        return MetadataUtils.copyMetadata(this.metadata);
    }

    /**
     * Stops the server insofar that it won't listen at the given port any more.
     */
    public void stopServer(){
        running = false;
        status.readWriteLock();
        status.setStopServer(true);
        status.readWriteUnlock();
    }

    public void initKVServer(TreeSet<ECSNode> metadata, int cacheSize, CachePolicy policy) {
        this.cacheSize = cacheSize;
        this.policy = policy;
        updateMetadata(metadata);

        for (ECSNode node : metadata) {
            logger.info(node.getPort() + node.getIP() + node.getHashedValue());
        }

        String storagePath = System.getProperty("user.dir") + File.separator + "storage";
        try {
            File storageFolder = new File(storagePath);
            if (!storageFolder.exists()) {
                if (!storageFolder.mkdir()) {
                    System.out.println("Could not create folder " + storagePath + ". Please check your permissions.");
                    System.exit(1);
                }
            } else {
                if(!storageFolder.isDirectory()) {
                    System.out.println(storagePath + " is detected as not a folder. Please either rename or delete the existing file.");
                    System.exit(1);
                }
            }
        } catch (SecurityException se) {
            System.out.println("Check write permissions for " + System.getProperty("user.dir"));
            System.exit(1);

        }

        try { 
            this.storageManager = new StorageManager(this.policy, cacheSize, storagePath, status);
        } catch (FileNotFoundException fnfe) {
            System.out.println("Could not find " + storagePath + ". Please try again");
            System.exit(1);
        }
    }

    private boolean createSocket() {
        logger.info("Initialize server ...");
        try {
            serverSocket = new ServerSocket(port);
            logger.info("Server listening on port: " 
                    + serverSocket.getLocalPort());    
            return true;
        
        } catch (IOException e) {
            logger.error("Error! Cannot open server socket:");
            if(e instanceof BindException){
                logger.error("Port " + port + " is already bound!");
            }
            return false;
        }
    }
    
    public StorageManager getStorageManager() {
        return this.storageManager;
    }
    
    public int getPort() {
        return this.port;
    }

    /**
     * Main entry point for the echo server application. 
     * @param args contains the port number at args[0].
     */
    public static void main(String[] args) {
        try {
            new LogSetup("logs/server/server.log", Level.ALL);
            if(args.length != 1) {
                System.out.println("Error! Invalid number of arguments!");
                System.out.println("Usage: Server <port>!");
            } else {
                int port = Integer.parseInt(args[0]);
                /*
                UNCOMMENT THESE LINES IF YOU WANT TO TEST WITH LEGACY CONSTRUCTOR.
                ECSNode node = new ECSNode("8123", "127.0.0.1");
                Comparator<ECSNode> comparator = new hashRingComparator();
                TreeSet<ECSNode> metadata = new TreeSet<ECSNode>(comparator);
                metadata.add(node);
                new KVServer(port, 256, "FIFO", metadata).start();
                */
                new KVServer(port).start();
            }
        } catch (IOException e) {
            System.out.println("Error! Unable to initialize logger!");
            e.printStackTrace();
            System.exit(1);
        } catch (NumberFormatException nfe) {
            System.out.println("Error! Invalid argument <port>! Not a number!");
            System.out.println("Usage: Server <port>!");
            System.exit(1);
        }
    }
}
