package datastore;

import cache.Cache;
import cache.CacheManager;
import cache.CachePolicy;
import storage.Storage;

import common.messages.*;

import logger.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.FileNotFoundException;

/**
* Handles acceses to storage (which includes the cache).
*/
public class StorageManager {
   
    private CacheManager cacheManager;
    private Storage storage;
    
    private static Logger logger = Logger.getLogger(StorageManager.class.getName());
    
    /**
    * Creates a new StorageManager with a given cache policy, cache size, and path to disk for persistent storage.
    * @param policy The cache policy to be used for the in-memory cache.
    * @param cacheSize The maximum number of key-value pairs that can be held in the cache.
    * @param storageDirectory The path where a the folder "storage" will be created for persistent storage.
    * @throws FileNotFoundException If it is unable to find the path indicated by storageDirectory.
    */
    public StorageManager(CachePolicy policy, int cacheSize, String storageDirectory) throws FileNotFoundException {
        super();
        this.cacheManager =  new CacheManager(policy, cacheSize); 
        this.storage = new Storage(storageDirectory, this.cacheManager);
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
    public String get(String key) {
        if (key == null) {
            logger.info("Server GET rejecting null key");
            return null;
        }
        logger.info("Server GET with Key: " + key);
        return storage.get(key);    
    }

    /**
    * Inserts a key-value pair.
    * @param key The key belonging to the key-value pair.
    * @param value The value belonging to the key-value pair.
    * @return A {@link StatusType} indicating the status of the insert opertation.
    */
    public StatusType set(String key, String value){
        if (key == null) {
            logger.info("Server PUT rejecting null key");
            return StatusType.PUT_ERROR;
        }

        logger.info("Server SET with Key: " + key + " Value: " + value);

        StatusType status = storage.put(key, value);
        return status;
    }

    /**
    * Deletes a key-value pair.
    * @param key The key belonging to the key-value pair.
    * @return A {@link StatusType} indicating the status of the delete operation.
    */
    public StatusType delete(String key) {
        if (key == null) {
            logger.info("Server DELETE rejecting null key");
            return StatusType.DELETE_ERROR;
        }
    
        logger.info("Server DELETE with Key: " + key);

        StatusType status = storage.delete(key);
        return status;
    }
}
