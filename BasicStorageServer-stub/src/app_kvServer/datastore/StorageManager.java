package datastore;

import cache.Cache;
import cache.CacheManager;
import cache.CachePolicy;
import storage.Storage;
import app_kvServer.*;
import app_kvEcs.*;

import common.*;
import common.messages.*;

import logger.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.*;

/**
* Handles acceses to storage (which includes the cache).
*/
public class StorageManager {
   
    private CacheManager cacheManager;
    private Storage storage;
    private KVServerStatus serverStatus; 
    private static Logger logger = Logger.getLogger(StorageManager.class.getName());
    
    /**
    * Creates a new StorageManager with a given cache policy, cache size, and path to disk for persistent storage.
    * @param policy The cache policy to be used for the in-memory cache.
    * @param cacheSize The maximum number of key-value pairs that can be held in the cache.
    * @param storageDirectory The path where a the folder "storage" will be created for persistent storage.
    * @throws FileNotFoundException If it is unable to find the path indicated by storageDirectory.
    */
    public StorageManager(CachePolicy policy, int cacheSize, String storageDirectory, KVServerStatus serverStatus) throws FileNotFoundException {
        super();
        this.cacheManager =  new CacheManager(policy, cacheSize); 
        this.storage = new Storage(storageDirectory, this.cacheManager);
        this.serverStatus = serverStatus;
        try {
            new LogSetup("logs/server/storageManager.log", Level.ALL);
        } catch (IOException e) {
            System.out.println("Error! Unable to initialize logger!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
    * Retrieves a value for a specified key if the key exists.
    * @param key The key associated with the desired value.
    * @return The value associated with the key. Returns null if key does not exist or an error occurs. 
    */ 
    public KVMessage get(String key, int version) {
        serverStatus.readReadLock();
        if (serverStatus.stopServer()) {
            serverStatus.readReadUnlock();
            //TODO(LOUIS): NEED TO RETURN SOME STATUS TYPE HERE
            return new KVMessage(CommandType.GET).setStatus(StatusType.SERVER_STOPPED);
        }
        serverStatus.metadataReadLock();
        try {
            ECSNode successor = MetadataUtils.getSuccessor(MetadataUtils.hash(key), serverStatus.getMetadata()); 
            if (serverStatus.getPort() != Integer.parseInt(successor.getPort())) {
                // Need to reroute the read.
                TreeSet<ECSNode> metadata = serverStatus.getMetadata();
                serverStatus.metadataReadUnlock();
                serverStatus.readReadUnlock();
                // TODO(LOUIS): NEED TO RETURN SOME STATUS TYPE HERE
                return new KVMessage(CommandType.GET).setStatus(StatusType.REROUTE).setMetadata(metadata);
            }
        } catch (Exception e) {
            serverStatus.metadataReadUnlock();
            serverStatus.readReadUnlock();
            // TODO(LOUIS): NEED TO RETURN SOME STATUS TYPE HERE
            return new KVMessage(CommandType.GET).setStatus(StatusType.ERROR);
        }
        serverStatus.metadataReadUnlock();  
        // Don't need to reroute the read.

        logger.info("Server GET with Key: " + key);
        serverStatus.readReadUnlock();
        String value = storage.get(key);
        if (value == null) {
            return new KVMessage(CommandType.GET).setStatus(StatusType.GET_ERROR);  
        }
        return new KVMessage(CommandType.GET).setStatus(StatusType.GET_SUCCESS).setValue(value); 
    }

    /**
    * Inserts a key-value pair.
    * @param key The key belonging to the key-value pair.
    * @param value The value belonging to the key-value pair.
    * @return A {@link StatusType} indicating the status of the insert opertation.
    */
    public StatusType set(String key, String value, int version){
        serverStatus.versionReadLock();
        if (version != serverStatus.getVersion()) {
            serverStatus.versionReadUnlock();
            return StatusType.SERVER_WRITE_LOCK;
        }
        if (key == null) {
            logger.info("Server PUT rejecting null key");
            serverStatus.versionReadUnlock();
            return StatusType.PUT_ERROR;
        }

        logger.info("Server SET with Key: " + key + " Value: " + value);

        StatusType status = storage.put(key, value);
        serverStatus.versionReadUnlock();
        return status;
    }

    /**
    * Deletes a key-value pair.
    * @param key The key belonging to the key-value pair.
    * @return A {@link StatusType} indicating the status of the delete operation.
    */
    public StatusType delete(String key, int version) {
        serverStatus.versionReadLock();
        if (version != serverStatus.getVersion()) {
            serverStatus.versionReadUnlock();
            return StatusType.SERVER_WRITE_LOCK;
        }
        if (key == null) {
            logger.info("Server DELETE rejecting null key");
            serverStatus.versionReadUnlock();
            return StatusType.DELETE_ERROR;
        }
    
        logger.info("Server DELETE with Key: " + key);

        StatusType status = storage.delete(key);
        serverStatus.versionReadUnlock();
        return status;
    }


    public boolean blockReadsAndShutdown() {
        serverStatus.readWriteLock();
        serverStatus.setStopServer(true);
        serverStatus.readWriteUnlock();
        return true;
    }
}
