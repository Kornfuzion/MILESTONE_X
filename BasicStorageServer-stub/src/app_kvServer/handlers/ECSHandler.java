package handlers;

import app_kvServer.*;
import cache.*;
import common.messages.*;
import org.apache.log4j.*;

public class ECSHandler implements MessageHandler {
    private final ClientType type = ClientType.ECS;
    private static Logger logger = Logger.getRootLogger();
    private RequestConnection requestConnection;
    private KVServer server;

    public ECSHandler(RequestConnection requestConnection, KVServer server) {
        this.requestConnection = requestConnection;
        this.server = server;
    }

    public ClientType getClientType() {
        return this.type;
    }

    public KVMessage handleMessage(KVMessage message) throws Exception {
        StatusType responseStatus;
        switch (message.getCommand()) {
            case INIT:
                server.initKVServer(message.getMetadata(), message.getCacheSize(), message.getCachePolicy());
                requestConnection.setPersistence(true);
                break;
            case START:
                server.startServer();
                break;
            case STOP:
                server.stopServer();
                break;
            case SHUT_DOWN:
                server.shutdownServer();
                break;
            case LOCK_WRITE:
                server.lockWrite();
                break;
            case UNLOCK_WRITE:
                server.unlockWrite();
                break;
            case MOVE_DATA:
                server.moveData();
                break;
            case UPDATE_METADATA:
                server.updateMetadata(message.getMetadata());
                break;
        }

        // For now, assume no failures possible so just return a generic success
        // Especially since the server will simply die if it fails to execute 
        // For example, in the case of initKVServer
        return new KVMessage(message.getCommand())
                    .setStatus(StatusType.SUCCESS);

    }    
}
