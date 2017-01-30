package client;

import app_kvClient.client.*;
import common.messages.KVMessage;

public class KVStore implements KVCommInterface {

        private KVClient client;
        private String address;
        private int port;	
	/**
	 * Initialize KVStore with address and port of KVServer
	 * @param address the address of the KVServer
	 * @param port the port of the KVServer
	 */
	public KVStore(String address, int port) {
	    this.address = address;
            this.port = port;
        }
	
	@Override
	public void connect() throws Exception {
		// TODO Auto-generated method stub
	        client = new KVClient(address, port);	
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
	        client.closeConnection();	
	}

        private void checkConnected() throws Exception {
                if (!client.isRunning()) {
                    throw new Exception();
                }
        }

	@Override
	public KVMessage put(String key, String value) throws Exception {
                checkConnected();
                if (value == null) {
                    client.delete(key);
                }
                else {
		    client.put(key, value);
                }
                return client.getLatestMessage(); 
	}

	@Override
	public KVMessage get(String key) throws Exception {
                checkConnected(); 
                client.get(key);
                return client.getLatestMessage();
	}

	
}
