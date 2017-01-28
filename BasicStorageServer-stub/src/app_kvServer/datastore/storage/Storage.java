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

    public Storage(String storagePath) throws FileNotFoundException, NullPointerException {
        this.storagePath = storagePath;
        fileLocks = new ConcurrentHashMap<String, ReadWriteLock>();

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
        ReadWriteLock checkLock = fileLocks.get(key);

        if ((checkLock == null) || (checkLock != rwLock)) {
            // Another thread came after and deleted something.
            // This read can be considered stale and return null (assuming the other thread doesn't crash). 
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

    public boolean put(String key, String value) {
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
                        newRWLock.writeLock().unlock();
                        return true; // Not really true but... 
                    }

                    boolean write = writeToFile(key, value);
                    newRWLock.writeLock().unlock();
                    return write;                        
                } else { // lock already exists
                    fileLock.writeLock().lock();
                    ReadWriteLock checkLock = fileLocks.get(key);
                    if ((checkLock == null) || (checkLock != fileLock)) {
                        // Another thread came after and was either a delete or another put.
                        // We can safely abort this put (assuming the other thread doesn't crash).
                        fileLock.writeLock().unlock();
                        return true; // Not really true but... 
                    }

                    boolean write = writeToFile(key, value);
                    fileLock.writeLock().unlock();
                    return write; 
                }
            } catch (NullPointerException npe) {
                // Will go here if the the key is null.
                newRWLock.writeLock().unlock();
                if (fileLock != null) {
                    fileLock.writeLock().unlock();
                }
                return false; 
            }
        } else {    // key exists in the hash map
            rwLock.writeLock().lock();
            ReadWriteLock checkLock = fileLocks.get(key);
            if((checkLock == null) || (checkLock != rwLock)) {
                // Another thread came after and was either a delete or another put.
                // We can safely abort this put (assuming the other thread doesn't crash).
                rwLock.writeLock().unlock();
                return true; // Not really true but...
            }
            
            boolean write = writeToFile(key, value);
            rwLock.writeLock().unlock();
            return write;
        }
    }

    public boolean delete(String key) {
        ReadWriteLock rwLock = fileLocks.get(key);

        // Key does not exist.
        if (rwLock == null) {
            return true; //Should probably return that key does not exist
        }


        rwLock.writeLock().lock();
        ReadWriteLock checkLock = fileLocks.get(key);
        if ((checkLock == null) || (checkLock != rwLock)) {
            // Another thread came after and either deleted or added something.
            // This delete can be aborted safely (assuming the other thread doesn't crash). 
            rwLock.writeLock().unlock(); 
            return true; // Technically not true but...               
        }

        try {
            String filePath = storagePath + File.separator + key;
            File file = new File(filePath);
            
            // Return value of delete() does not matter.
            file.delete();
    
            // Now we can delete the keys from both the fileLocks and writerLocks maps.
            try {
                fileLocks.remove(key);

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
