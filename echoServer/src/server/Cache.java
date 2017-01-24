package ece419StorageManager;

public class Cache {
	private long timesUsed;
	private String key;
	private String value;
	
	public Cache(long timesUsed, String key, String value) {
		super();
		this.timesUsed = timesUsed;
		this.key = key;
		this.value = value;
	}
	//getters
	public long getTimesUsed() {
		return timesUsed;
	}
	public String getKey() {
		return key;
	}
	public String getValue() {
		return value;
	}
	//setters
	public void setTimesUsed(long timesUsed) {
		this.timesUsed = timesUsed;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
}
