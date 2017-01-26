package datastore;

import cache.Cache;
import cache.CacheManager;
import storage.Storage;

public class StorageManager {
	
	private CacheManager cacheManager;
	private Storage	storage;
	
	//constructor
	public StorageManager(int policy, int cacheSize) {
		super();
		this.cacheManager =  new CacheManager(policy, cacheSize);
		//this.storage = new Storage();
	}
	
	public String get(String key){
		String s = cacheManager.get(key);
		if(s == null){
			//do storage get
		}
		return s;
	}
	public boolean set(String key, String value){
		cacheManager.update(key, value);
		//do storage update
		return false;
	}
	public boolean delete(String key){
		cacheManager.delete(key);
		//do storage delete
		return false;
	}
	
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
}
