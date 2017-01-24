package ece419StorageManager; //TODO WE change this later LOIS!

public interface CacheQueue {
		
	public boolean find(String Key);
	
	public String getValue(String Key);
	
	public boolean cachePush(String Key, String Value);
	
	public boolean cachePop(String Key);
	
	public boolean setValue(String Key, String newValue);
	
}
