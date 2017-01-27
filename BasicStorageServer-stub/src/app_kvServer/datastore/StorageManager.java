package datastore;

import cache.Cache;
import cache.CacheManager;
import cache.CachePolicy;
import storage.Storage;

import logger.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.io.IOException;

public class StorageManager {
   
    private CacheManager cacheManager;
    private Storage storage;
	
	private static Logger logger = Logger.getLogger(StorageManager.class.getName());

    
    //constructor
    public StorageManager(CachePolicy policy, int cacheSize) {
        super();
        this.cacheManager =  new CacheManager(policy, cacheSize);
        // TODO(Louis): Undo this comment when ready.
        //this.storage = new Storage();
		try {
			new LogSetup("logs/server/storageManager.log", Level.ALL);
		} catch (IOException e) {
			System.out.println("Error! Unable to initialize logger!");
			e.printStackTrace();
			System.exit(1);
			}
    }
    
    public String get(String key){
		if(key != null){
			logger.info("Server GET with Key: " + key);
		}
        String s = cacheManager.get(key);
		if(s != null)
			logger.info("Server GET found Key, Value pair... Key: " + key + " Value: " + s);
		else
			logger.info("Server could not find key value pair...");
        return s;
    }
    public boolean set(String key, String value){
        logger.info("Server PUT updated cache with Key: " + key + " Value: " + value);		
        return cacheManager.update(key, value);
    }
    public boolean delete(String key){
        logger.info("Server DELETE in cache with Key: " + key);		
        return cacheManager.delete(key);
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
