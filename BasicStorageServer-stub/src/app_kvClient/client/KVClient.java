package app_kvClient.client;

import common.messages.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

import org.apache.log4j.Logger;

import app_kvClient.client.ClientSocketListener.SocketStatus;

public class KVClient extends Thread {

	private Logger logger = Logger.getRootLogger();
	private Set<ClientSocketListener> listeners;
	private boolean running;
	
	private Socket clientSocket;
	private OutputStream output;
 	private InputStream input;
	
	public KVClient(String address, int port) 
			throws UnknownHostException, IllegalArgumentException, IOException {
		clientSocket = new Socket(address, port);
		listeners = new HashSet<ClientSocketListener>();
        setupIOStreams();	
		setRunning(true);
		logger.info("Connection established");
	}

    public KVMessage getLatestMessage() {
        try {
           return KVMessageUtils.receiveMessage(input);
        }
        catch(Exception e) {
            return null;
        }
    }

    public void setupIOStreams() throws IOException {
        output = clientSocket.getOutputStream();
        input = clientSocket.getInputStream();
    }
	
	/**
	 * Initializes and starts the client connection. 
	 * Loops until the connection is closed or aborted by the client.
	 */
	public void run() {
		try {
			while(isRunning()) {
				try {
				    KVMessage latestMsg = KVMessageUtils.receiveMessage(input);
					for(ClientSocketListener listener : listeners) {
						listener.handleNewMessage(latestMsg);
					}
				} catch (Exception ioe) {
					if(isRunning()) {
						logger.error("Connection lost!");
						try {
							tearDownConnection();
							for(ClientSocketListener listener : listeners) {
								listener.handleStatus(
										SocketStatus.CONNECTION_LOST);
							}
						} catch (Exception e) {
							logger.error("Unable to close connection!");
						}
					}
				}				
			}
		} catch (Exception e) {
			logger.error("Connection could not be established!");
			
		} finally {
			if(isRunning()) {
				closeConnection();
			}
		}
	}
	
	public synchronized void closeConnection() {
		logger.info("try to close connection ...");
		
		try {
			tearDownConnection();
			for(ClientSocketListener listener : listeners) {
				listener.handleStatus(SocketStatus.DISCONNECTED);
			}
		} catch (IOException ioe) {
			logger.error("Unable to close connection!");
		}
	}
	
	private void tearDownConnection() throws IOException {
		setRunning(false);
		logger.info("tearing down the connection ...");
		if (clientSocket != null) {
			input.close();
			output.close();
			clientSocket.close();
			clientSocket = null;
			logger.info("connection closed!");
		}
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void setRunning(boolean run) {
		running = run;
	}
	
	public void addListener(ClientSocketListener listener){
		listeners.add(listener);
	}
	
    public void initMetadata() throws Exception {
        KVMessageUtils.sendMessage(new KVMessage(CommandType.INIT_CLIENT_METADATA), output);
    }

    public void get(String key) throws Exception {
        System.out.println(key + (output==null));
        KVMessageUtils.sendMessage(KVMessage.createGetRequest(key), output);
    }

    public void put(String key, String value) throws Exception {
        KVMessageUtils.sendMessage(KVMessage.createPutRequest(key, value), output);
    }

    public void delete(String key) throws Exception {
        KVMessageUtils.sendMessage(KVMessage.createDeleteRequest(key), output);
    }

    public void sendChatMessage(String msg) throws Exception {
        KVMessageUtils.sendMessage(KVMessage.createChatMessage(msg), output);
    }
}
