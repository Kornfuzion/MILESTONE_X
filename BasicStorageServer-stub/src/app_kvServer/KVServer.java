package app_kvServer;

import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.FileNotFoundException;

import app_kvEcs.*;
import cache.*;
import common.*;
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
    private TreeSet<ECSNode> metadata;
    private ReadWriteLock writeLock;
    private boolean isWriteLocked;
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
        this.writeLock = new ReentrantReadWriteLock();
        this.isWriteLocked = false;
    }

    // Leave this constructor as legacy, but from now on we will only be using the constructor with port number
    // The ECS client shall start each server with its respective port and initialize through messages
    public KVServer(int port, int cacheSize, String policy) {
        this.port = port;
        this.cacheSize = cacheSize;
        this.policy = CachePolicy.parseString(policy);
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
            this.storageManager = new StorageManager(this.policy, cacheSize, storagePath);
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
                    RequestConnection connection = new RequestConnection(this, client, writeLock, storageManager);
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
    }

    // These don't do shit
    // Essentially phantom calls - so we'll call them and reply instantly
    public void lockWrite() {
        writeLock.writeLock().lock();    
        isWriteLocked = true;
    }
    public void unlockWrite() {}
    public void moveData() {}

    public boolean isWriteLocked() {
        return isWriteLocked;
    }

    public void updateMetadata(TreeSet<ECSNode> metadata) {
        this.metadata = metadata;
    }

    // Return a copy of the metadata, to be safe
    public TreeSet<ECSNode> getMetadata() {
        return MetadataUtils.copyMetadata(this.metadata);
    }

    public boolean isSuccessor(ECSNode successor) {
        return this.port == Integer.parseInt(successor.getPort());
    }

    /**
     * Stops the server insofar that it won't listen at the given port any more.
     */
    public void stopServer(){
        running = false;
    }

    public void initKVServer(TreeSet<ECSNode> metadata, int cacheSize, CachePolicy policy) {
        this.cacheSize = cacheSize;
        this.policy = policy;
        this.metadata = metadata;

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
            this.storageManager = new StorageManager(this.policy, cacheSize, storagePath);
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
                //new KVServer(port, 256, "FIFO").start();
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
