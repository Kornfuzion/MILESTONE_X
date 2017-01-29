package storage;

import cache.CacheManager;

import common.messages.status.StatusType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import logger.LogSetup;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
* Handles caching and persistence of key-value pairs.
*/
public class Storage {

    private CacheManager cacheManager;
    private String storagePath;
    private ConcurrentHashMap<String, ReadWriteLock> fileLocks;

    private static Logger logger = Logger.getLogger(Storage.class.getName());

    /**
    * Creates a new Storage with a given path to persist onto disk and a cache manager.
    * @param storagePath The path to the directory where files will be persisted onto disk.
    * @param cacheManager The {@link CacheManager} used to cache key-value pairs in-memory. 
    */
    public Storage(String storagePath, CacheManager cacheManager) throws FileNotFoundException, NullPointerException {
        this.storagePath = storagePath;
        this.cacheManager = cacheManager;
        fileLocks = new ConcurrentHashMap<String, ReadWriteLock>();

        try {
            new LogSetup("logs/server/storage.log", Level.ALL);
        } catch (IOException e) {
            System.out.println("Error! Unable to initialize logger!");
            e.printStackTrace();
            System.exit(1);
        }
        // Read in existing keys = filename and create the RW locks
        
        File storageFolder = new File(this.storagePath);
        // If the the file does not exist or is not a directory then we abort the
        // startup of the server.
        if (!storageFolder.exists() || !storageFolder.isDirectory()) {
            throw new FileNotFoundException();
        }
        File[] storageFiles = storageFolder.listFiles();

        for (File file : storageFiles) {
            if (file != null) {
                fileLocks.putIfAbsent(file.getName(), new ReentrantReadWriteLock());
                logger.info("Reading in KEY: " + file.getName());
            } 
        } 
    }

    /**
    * Gets the value associated with the given key.
    * @param key The key associated with the desired value.
    * @return The value associated with the key. Returns null if the key does not exist or an error
    *         occurs reading from disk.
    */
    public String get(String key) {      
        if (key == null) {
            logger.info("GET: Illegal key <KEY = " + key +">");
            return null;
        }

        String value = null;
        ReadWriteLock rwLock = fileLocks.get(key);

        if (rwLock == null) {
            logger.info("GET: Key does not exist <KEY = " + key + ">");
            return null; // key does not exist. Should probably return an error code as well.
        }

        logger.info("GET: Key exists <KEY = " + key + ">");
        
        rwLock.readLock().lock();
        ReadWriteLock checkLock = fileLocks.get(key);

        if ((checkLock == null) || (checkLock != rwLock)) {
            // Another thread came after and deleted something.
            // This read can be considered stale and return null (assuming the other thread doesn't crash). 
            logger.info("GET: Stale read <KEY = " + key + ">"); 
            rwLock.readLock().unlock(); 
            return null; // key does not exist.               
        }

        value = cacheManager.get(key);

        if (value != null) {    // found in cache.
            logger.info("GET: Cache hit <KEY = " + key + ", VALUE = " + value +">");
            rwLock.readLock().unlock();
            return value;
        }
        
        logger.info("GET: Cache miss <KEY = " + key + ">"); 

        BufferedReader br = null;
        // At this point the file must exist on disk.
        try {
            br = new BufferedReader(new FileReader(storagePath + File.separator + key));
            value = br.readLine();
            br.close();
            logger.info("GET: Value read from disk <KEY = " + key + ", VALUE = " + value + ">");
        } catch (IOException e) {
            logger.info("GET: Failed file IO <KEY = " + key + ">");
            rwLock.readLock().unlock();
            return null; // should return error code that unexpected error happened.            
        } 

        // Does not really matter if this succeeds or not.
        boolean success = cacheManager.update(key, value);
    
        if (success) {
            logger.info("GET: Inserted into cache  <KEY = " + key + ", VALUE = " + value + ">");
        } else {
            logger.info("GET: Failed to insert into cache  <KEY = " + key + ", VALUE = " + value + ">");
        }
        
        rwLock.readLock().unlock();
        return value;                
    }


    private boolean writeToFile(String key, String value) {
        try {
            String filePath = storagePath + File.separator + key;
            File file = new File(filePath);

            // Return of createNewFie() not important.
            file.createNewFile();

            BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
            bw.write(value);
            bw.close();

            return true;
        } catch (IOException e) {
            return false;
        } catch (SecurityException se) {
            return false;
        }
    }

    /**
    * Inserts a key-value pair into the cache and persists it onto disk.
    * @param key The key associated with the key-value pair.
    * @param value The value associated with the key-value pair.
    * @return A {@link StatusType} indicating the status of the insert operation.
    */
    public StatusType put(String key, String value) {
        if ((key == null) || (value == null)) {
            logger.info("PUT: Illegal key/value <KEY = " + key + ", VALUE = " + value + ">");
            return StatusType.PUT_ERROR;
        }

        ReadWriteLock rwLock = fileLocks.get(key);
        if (rwLock == null) {   // key does not exist
            ReadWriteLock newRWLock = new ReentrantReadWriteLock();
            ReadWriteLock fileLock = null;
            newRWLock.writeLock().lock();
            try {
                fileLock = fileLocks.putIfAbsent(key, newRWLock);  // only one thread will succeed.
                if (fileLock == null) { // successfully put in the key.
                    ReadWriteLock checkLock = fileLocks.get(key);
                    if ((checkLock == null) || (checkLock != newRWLock)) {
                        // Another thread came after and was either a delete or another put.
                        // We can safely abort this put (assuming the other thread doesn't crash).
                        logger.info("PUT: Overwritten put <KEY = " + key + ", VALUE = " + value + ">");
                        newRWLock.writeLock().unlock();
                        return StatusType.PUT_SUCCESS; // Not really true but... 
                    }
                    boolean write = writeToFile(key, value);

                    if (!write) {
                        logger.info("PUT: Failed to write to disk <KEY = " + key + ", VALUE = " + value + ">");
                        newRWLock.writeLock().unlock();
                        return StatusType.PUT_ERROR;
                    }
                    
                    logger.info("PUT: Successfully wrote to disk <KEY = " + key + ", VALUE = " + value + ">");

                    // Need to put into cache.
                    boolean success = cacheManager.update(key, value);
                    
                    if (!success) {
                        logger.info("PUT: Failed to insert into cache <KEY = " + key + ", VALUE = " + value + ">");
                        newRWLock.writeLock().unlock();
                        return StatusType.PUT_ERROR;
                    }

                    logger.info("PUT: Inserted into cache <KEY = " + key + ", VALUE = " + value + ">");

                    newRWLock.writeLock().unlock();
                    return StatusType.PUT_SUCCESS;                        
                } else { // lock already exists
                    fileLock.writeLock().lock();
                    ReadWriteLock checkLock = fileLocks.get(key);
                    if ((checkLock == null) || (checkLock != fileLock)) {
                        // Another thread came after and was either a delete or another put.
                        // We can safely abort this put (assuming the other thread doesn't crash).
                        logger.info("PUT: Overwritten put <KEY = " + key + ", VALUE = " + value + ">");
                        fileLock.writeLock().unlock();
                        return StatusType.PUT_UPDATE; // Not really true but... 
                    }

                    boolean write = writeToFile(key, value);

                    if (!write) {
                        logger.info("PUT: Failed to write to disk <KEY = " + key + ", VALUE = " + value + ">");
                        fileLock.writeLock().unlock();
                        return StatusType.PUT_ERROR;   
                    }

                    logger.info("PUT: Successfully wrote to disk <KEY = " + key + ", VALUE = " + value + ">");

                    // Need to put into cache.
                    boolean success = cacheManager.update(key, value);

                    if (!success) {
                        logger.info("PUT: Failed to insert into cache <KEY = " + key + ", VALUE = " + value + ">");
                        fileLock.writeLock().unlock();
                        return StatusType.PUT_ERROR;   
                    }

                    logger.info("PUT: Inserted into cache <KEY = " + key + ", VALUE = " + value + ">");

                    fileLock.writeLock().unlock();
                    return StatusType.PUT_UPDATE; 
                }
            } catch (NullPointerException npe) {
                logger.info("PUT: Key should not be null <KEY = " + key + ", VALUE = " + value + ">");
                // Will go here if the the key is null.
                newRWLock.writeLock().unlock();
                if (fileLock != null) {
                    fileLock.writeLock().unlock();
                }
                return StatusType.PUT_ERROR; 
            }
        } else {    // key exists in the hash map
            rwLock.writeLock().lock();
            ReadWriteLock checkLock = fileLocks.get(key);
            if((checkLock == null) || (checkLock != rwLock)) {
                // Another thread came after and was either a delete or another put.
                // We can safely abort this put (assuming the other thread doesn't crash).
                logger.info("PUT: Overwritten put <KEY = " + key + ", VALUE = " + value + ">");
                rwLock.writeLock().unlock();
                return StatusType.PUT_UPDATE; // Not really true but...
            }
            
            boolean write = writeToFile(key, value);

            if (!write) {
                logger.info("PUT: Failed to write to disk <KEY = " + key + ", VALUE = " + value + ">");
                rwLock.writeLock().unlock();
                return StatusType.PUT_ERROR;
            }

            logger.info("PUT: Successfully wrote to disk <KEY = " + key + ", VALUE = " + value + ">");
            
            // Need to put into cache.
            boolean success = cacheManager.update(key, value);

            if (!success) {
                logger.info("PUT: Failed to insert into cache <KEY = " + key + ", VALUE = " + value + ">");
                rwLock.writeLock().unlock();
                return StatusType.PUT_ERROR;
            }

            logger.info("PUT: Inserted into cache <KEY = " + key + ", VALUE = " + value + ">");
            
            rwLock.writeLock().unlock();
            return StatusType.PUT_UPDATE;
        }
    }

    /**
    * Deletes the key-value pair specified by the key.
    * @param key The key associated with the key-value pair to be deleted.
    * @return A {@link StatusType} indicating the status of the delete operation.
    */
    public StatusType delete(String key) {
        if (key == null) {
            logger.info("DELETE: Illegal key <KEY = " + key +">");
            return StatusType.DELETE_ERROR;
        }
        ReadWriteLock rwLock = fileLocks.get(key);

        // Key does not exist.
        if ((rwLock == null)) {
            logger.info("DELETE: Key does not exist  <KEY = " + key +">");
            return StatusType.DELETE_ERROR; //Should probably return that key does not exist
        }

        rwLock.writeLock().lock();
        ReadWriteLock checkLock = fileLocks.get(key);
        if ((checkLock == null) || (checkLock != rwLock)) {
            // Another thread came after and either deleted or added something.
            // This delete can be aborted safely (assuming the other thread doesn't crash).
            logger.info("DELETE: Overwritten delete  <KEY = " + key +">"); 
            rwLock.writeLock().unlock(); 
            return StatusType.DELETE_SUCCESS; // Technically not true but...               
        }

        try {
            String filePath = storagePath + File.separator + key;
            File file = new File(filePath);
            
            // Return value of delete() does not matter.
            file.delete();
            logger.info("DELETE: Key/Value deleted from disk  <KEY = " + key +">");
            // Now we can delete the keys from both the fileLocks and writerLocks maps.
            try {
                fileLocks.remove(key);

                // Need to delete from cache.
                boolean success = cacheManager.delete(key);
                
                if (!success) {
                    logger.info("DELETE: Could not remove from cache  <KEY = " + key +">");
                    rwLock.writeLock().unlock();
                    return StatusType.DELETE_ERROR;
                }
                logger.info("DELETE: Removed from cache  <KEY = " + key +">");
                rwLock.writeLock().unlock();
                return StatusType.DELETE_SUCCESS;
            } catch (NullPointerException npe) {    // Shouldn't go here as long as key is not null
                logger.info("DELETE: Key should not be null  <KEY = " + key +">");
                rwLock.writeLock().unlock();
                return StatusType.DELETE_ERROR;
            }

        } catch (SecurityException se) {
            logger.info("DELETE: Insufficient permissions to delete from disk  <KEY = " + key +">");
            rwLock.writeLock().unlock();
            return StatusType.DELETE_ERROR;
        }
    }
}
