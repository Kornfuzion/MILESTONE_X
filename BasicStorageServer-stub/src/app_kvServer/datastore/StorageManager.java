package datastore;

import cache.Cache;
import cache.CacheManager;
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
    public StorageManager(int policy, int cacheSize) {
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
        if(s == null){
			logger.info("Server could not find in cache Key: " + key + " Looking into storage");
            //do storage get
        }
		if(s != null)
			logger.info("Server GET found Key, Value pair... Key: " + key + " Value: " + s);
		else
			logger.info("Server could not find key value pair...");
        return s;
    }
    public boolean set(String key, String value){
        if(cacheManager.update(key, value)){
			logger.info("Server PUT updated cache with Key: " + key + " Value: " + value);		
		}
		logger.info("Server PUT into storage... Key: " + key + " Value: " + value);		
        //do storage update
		return true; //take out later
        //return false;
    }
    public boolean delete(String key){
        if(cacheManager.delete(key)){
			logger.info("Server DELETE SUCCESSFUL in cache with Key: " + key);				
		}
		logger.info("Server DELETE SUCCESSFUL in cache with Key: " + key);	
        //do storage delete
		return true;	//take it out later
        //return false;
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
