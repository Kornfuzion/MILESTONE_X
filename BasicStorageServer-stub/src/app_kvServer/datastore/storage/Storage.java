package storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
            return null; // should return error code that unexpected error happened.            
        } 

        rwLock.readLock().unlock();
        return value;                
    } 
}
