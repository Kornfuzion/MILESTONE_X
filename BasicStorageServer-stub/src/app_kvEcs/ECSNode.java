package app_kvEcs;

import common.*;
import java.security.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;

public class ECSNode {
	private String port;
	private String IP;
	private String hashedValue;
	boolean alive;
	
	public ECSNode(String port, String IP){
		this.port = port;
		this.IP = IP;
		this.alive = true;
		try{
            this.hashedValue = MetadataUtils.hash(port + IP);
		} catch (Exception e){
		    e.printStackTrace();
		}
	}

	public String getPort(){
		return port;
	}
	
	public String getIP(){
		return IP;
	}

    public void setHashedValue(String hashedValue) {
        this.hashedValue = hashedValue;
    }

	public String getHashedValue(){
		return hashedValue;
	}

	public void setNodeDead(){
		this.alive = false;
	}

	public void setNodeAlive(){
		this.alive = true;	
	}

	public boolean getLiveness(){
		return this.alive;	
	}
}
