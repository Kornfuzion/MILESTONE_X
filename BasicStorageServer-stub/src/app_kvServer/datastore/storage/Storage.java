package storage;

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

public class Storage {

    private String storagePath;
    private ConcurrentHashMap<String, ReadWriteLock> fileLocks;
    private ConcurrentHashMap<String, ReadWriteLock> writerLocks;

    public Storage(String storagePath) throws FileNotFoundException, NullPointerException {
        this.storagePath = storagePath;
        fileLocks = new ConcurrentHashMap<String, ReadWriteLock>();
        writerLocks = new ConcurrentHashMap<String, ReadWriteLock>();       

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
            } 
        } 
    }

    public String get(String key) {
        String value = null;
        ReadWriteLock rwLock = fileLocks.get(key);

        if (rwLock == null) {
            return null; // key does not exist. Should probably return an error code as well.
        }
        
        rwLock.readLock().lock();
        // Check if another client has deleted the file already.
        if (!fileLocks.containsKey(key)) {
            rwLock.readLock().unlock();
            return null; // key does not exist.               
        }
            
        BufferedReader br = null;
        // At this point the file must exist on disk.
        try {
            br = new BufferedReader(new FileReader(storagePath + File.separator + key));
            value = br.readLine();
            br.close();
        } catch (IOException e) {
            rwLock.readLock().unlock();
            return null; // should return error code that unexpected error happened.            
        } 

        rwLock.readLock().unlock();
        return value;                
    }

    public boolean put(String key, String value) {
        ReadWriteLock rwLock = null;
        ReadWriteLock newRWLock = new ReentrantReadWriteLock();
        try {
            rwLock = writerLocks.putIfAbsent(key, newRWLock);

            // Successfully put in key.
            if (rwLock == null) {
                newRWLock.writeLock().lock();

                // Write to file.
                try {
                    String filePath = storagePath + File.separator + key;
                    File file = new File(filePath);

                    // At this time the file does not exist on disk.
                    file.createNewFile();
                    
                    BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
                    bw.write(value);
                    bw.close();

                    // We have successfully written to disk, need to update fileLocks map
                    // to make the key visible to all other clients. 
                    try {
                        fileLocks.put(key, newRWLock); 
                    } catch (NullPointerException npe1) {
                        newRWLock.writeLock().unlock();
                        return false;
                    }

                    newRWLock.writeLock().unlock();
                    return true; 
                } catch (IOException e) {
                    newRWLock.writeLock().unlock();
                    return false;
                } catch (SecurityException se) {
                    newRWLock.writeLock().unlock();
                    return false;
                } 
            } else {    // key already exists.
                rwLock = writerLocks.get(key);

                if (rwLock == null) {   // should never get here.
                    return false;   // Log a very weird error.
                }

                rwLock.writeLock().lock();

                try {
                    String filePath = storagePath + File.separator + key;
                    File file = new File(filePath);
    
                    // File should already be on disk at this point, but just in case...
                    // Return value of createNewFile() does not matter in this case.
                    file.createNewFile();
                    
                    BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
                    bw.write(value);
                    bw.close();

                    rwLock.writeLock().unlock();
                    return true;
                } catch (IOException e) {
                    rwLock.writeLock().unlock();
                    return false;
                } catch (SecurityException se) {
                    rwLock.writeLock().unlock();
                    return false;
                }           
            }

        } catch (NullPointerException npe2) {
            if (rwLock != null) {
                rwLock.writeLock().unlock();
            }
            if (newRWLock != null) {
                newRWLock.writeLock().unlock();
            }
            // If key or value is null.
            return false;
        }         
    }

    public boolean delete(String key) {
        ReadWriteLock rwLock = fileLocks.get(key);

        // Key does not exist.
        if (rwLock == null) {
            return true; //Should probably return that key does not exist
        }

        rwLock.writeLock().lock();
        try {
            String filePath = storagePath + File.separator + key;
            File file = new File(filePath);
            
            // Return value of delete() does not matter.
            file.delete();
    
            // Now we can delete the keys from both the fileLocks and writerLocks maps.
            try {
                fileLocks.remove(key);
                writerLocks.remove(key);

                rwLock.writeLock().unlock();
                return true;
            } catch (NullPointerException npe) {    // Shouldn't go here as long as key is not null
                rwLock.writeLock().unlock();
                return false;
            }
        } catch (SecurityException se) {
            rwLock.writeLock().unlock();
            return false;
        }
    }
}
