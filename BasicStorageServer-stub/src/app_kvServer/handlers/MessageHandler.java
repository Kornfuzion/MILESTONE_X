package handlers;

import common.messages.*;

public interface MessageHandler {
    public ClientType getClientType();	
	public KVMessage handleMessage(KVMessage message) throws Exception;
}
