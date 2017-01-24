package ece419StorageManager;

public interface CacheQueue {
		
	public boolean find(String Key);
	
	public String getValue(String Key);
	
	public boolean cachePush(String Key, String Value);
	
	public boolean cachePop(String Key);
	
	public boolean setValue(String Key, String newValue);
	
}
