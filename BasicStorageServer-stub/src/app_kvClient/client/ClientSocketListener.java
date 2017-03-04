package app_kvClient.client;

import common.messages.*;

public interface ClientSocketListener {

	public enum SocketStatus{CONNECTED, DISCONNECTED, CONNECTION_LOST};
	
	public void handleNewMessage(KVMessage msg) throws Exception;
	
	public void handleStatus(SocketStatus status);
}
