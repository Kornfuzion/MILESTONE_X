package datastore;

import cache.Cache;
import cache.CacheManager;
import cache.CachePolicy;
import storage.Storage;

import common.messages.status.StatusType;

import logger.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.FileNotFoundException;

public class StorageManager {
   
    private CacheManager cacheManager;
    private Storage storage;
    
    private static Logger logger = Logger.getLogger(StorageManager.class.getName());
    
    //constructor
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
    
    public String get(String key) {
        if (key == null) {
            logger.info("Server GET rejecting null key");
            return null;
        }
        logger.info("Server GET with Key: " + key);

        return storage.get(key);
        
/*        
        // Check if it is in cache.
        String s = cacheManager.get(key);
        if(s != null) {
            logger.info("Server CACHE HIT: GET found Key, Value pair... Key: " + key + " Value: " + s);
            return s;
        }
         
        logger.info("Server CACHE MISS: Key: " + key);
    
        // Check if it is on disk.
        s = storage.get(key);
        if (s != null) {
            logger.info("Server GET found Key, Value pair... Key: " + key + " Value : " + s);
        } else {
            logger.info("Server GET Key not found: Key: " + key);
        }
        return s;    
*/
    }
    public StatusType set(String key, String value){
        if (key == null) {
            logger.info("Server PUT rejecting null key");
            return StatusType.PUT_ERROR;
        }
        StatusType status = storage.put(key, value);
        return status;
/*
        // Write to disk.
        StatusType status = storage.put(key, value);
        if (status == StatusType.PUT_ERROR) {
            logger.info("Server PUT failed with Key: " + key + " Value: " + value);
            return status;
        }    
        else {
            logger.info("Server PUT updated disk with Key: " + key + " Value: " + value);
        }
        boolean cacheSuccess = cacheManager.update(key, value);
        if (cacheSuccess) {
            logger.info("Server PUT updated cache with Key: " + key + " Value: " + value);
        } else {
            logger.info("Server PUT failed update to cache with Key: " + key + " Value: " + value);
        }
        return status;
*/
    }
    public StatusType delete(String key) {
        if (key == null) {
            logger.info("Server DELETE rejecting null key");
            return StatusType.DELETE_ERROR;
        }
        StatusType status = storage.delete(key);
        return status;
/*
        // Delete from disk.
        StatusType status = storage.delete(key);
        if (status == StatusType.DELETE_ERROR) {
            logger.info("Server DELETE failed with Key: " + key);
            return status;
        } else {
            logger.info("Server DELETE updated disk with Key: " + key);
        }

        boolean cacheSuccess = cacheManager.delete(key);
        if (cacheSuccess) {
            logger.info("Server DELETE updated cache with Key: " + key);
        } else {
            logger.info("Server DELETE failed to update cache with Key: " + key);
        }

        return status;
*/
    }

    /*
    public static void main(String[] args) {
        System.out.print("testing this shit kappa ");
        String string = "testing this string";
        int size = string.length();
        System.out.println(size);
        Cache d = new Cache(0,"testing","testing");
        CacheManager cacheManager = new CacheManager(1, 1000);
        CacheManager cacheManager2 = new CacheManager(2, 1000);
        CacheManager cacheManager3 = new CacheManager(3, 1000);
    }
    */
}
