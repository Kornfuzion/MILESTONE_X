package storage;

import java.io.FileInputStream;

public class StorageNode {
	private String key;
	private FileInputStream iStream;
	
	public StorageNode(String key, FileInputStream iStream) {
		super();
		this.key = key;
		this.iStream = iStream;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setiStream(FileInputStream iStream) {
		this.iStream = iStream;
	}

	public String getKey() {
		return key;
	}

	public FileInputStream getiStream() {
		return iStream;
	}
	
	
	
}
