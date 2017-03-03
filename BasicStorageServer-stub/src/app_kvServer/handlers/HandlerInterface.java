package app_kvServer.handlers;

import common.messages.*;

public interface HandlerInterface {
    public void getClientType();	
	public void handleMessage(KVMessage msg);
}
