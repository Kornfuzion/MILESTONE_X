package app_kvEcs;

import java.security.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;

public class ECSNode {
	private String port;
	private String IP;
	private String hashedValue;
	
	public ECSNode(String port, String IP){
		this.port = port;
		this.IP = IP;
		String t = port + IP;
		try{
			byte[] bytesOfMessage = t.getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(bytesOfMessage);
			//this.hashedValue = new String(digest, StandardCharsets.UTF_8); 
			final StringBuilder builder = new StringBuilder();
			for(byte b : digest) {
				builder.append(String.format("%02x", b));
			}
    		this.hashedValue = builder.toString();
		} catch (NoSuchAlgorithmException e){
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e){
		
		}
	}
	
	public String getPort(){
		return port;
	}
	
	public String getIP(){
		return IP;
	}

	public String getHashedValue(){
		return hashedValue;
	}

}
