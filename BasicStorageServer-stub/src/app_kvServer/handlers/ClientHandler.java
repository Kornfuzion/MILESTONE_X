package handlers;

import app_kvEcs.*;
import app_kvServer.*;
import common.*;
import common.messages.*;
import datastore.*;
import logger.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.log4j.*;

public class ClientHandler implements MessageHandler {
    private final ClientType type = ClientType.CLIENT;
    private static Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private StorageManager storageManager;
    private KVServer server;

    public ClientHandler(KVServer server, StorageManager storageManager) {
        this.server = server;
        this.storageManager = storageManager;
        try {
            new LogSetup("logs/server/server.log", Level.ALL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ClientType getClientType() {
        return this.type;
    }

    public KVMessage handleMessage(KVMessage message) throws Exception {
        KVServerStatus serverStatus = server.getServerStatus();
		// If the server isn't supposed to be accepting user requests yet,
        // Block the request, reply with an ERROR message
        if (!server.isRunning() && server.alive()) {
           return new KVMessage(message.getCommand())
                        .setStatus(StatusType.SERVER_STOPPED);
        }

        KVMessage response = new KVMessage(message.getCommand());
        StatusType responseStatus = StatusType.ERROR;
        String reply = "";

        int version = 0;
		// Checking for if server is write locked.
		if (message.getCommand() == CommandType.PUT || message.getCommand() == CommandType.DELETE) {
        	serverStatus.writeReadLock();
        	// Server is under write lock, return write lock message to client.
        	if (serverStatus.isWriteLocked()) {
            	serverStatus.writeReadUnlock();
            	responseStatus = StatusType.SERVER_WRITE_LOCK;
            	response.setStatus(responseStatus)
                    	.setMessage(reply);
            	logger.info("REPLIED TO " + message.getCommand() + " WITH STATUS " + response.getStatus());
            	return response;
        	}
        	// Server is not under write lock, get a version number.
        	serverStatus.versionReadLock();
        	version = serverStatus.getVersion();
        	serverStatus.versionReadUnlock();
        	serverStatus.writeReadUnlock();
		}

		serverStatus.metadataReadLock();
        ECSNode successor = MetadataUtils.getSuccessor(MetadataUtils.hash(message.getKey()), server.getMetadata());
        TreeSet<ECSNode> metadata = null;

        boolean reroute = !server.isSuccessor(successor, message.getCommand()) || !server.alive();
		serverStatus.metadataReadUnlock();

        if (reroute) {
            responseStatus = StatusType.REROUTE;
            metadata = server.getMetadata();
            reply = "INCORRECT SERVER: hashed key of [" + MetadataUtils.hash(message.getKey()) + "] served by server (port,IP) = (" + server.getPort() + ",127.0.0.1)" + " BUT SHOULD BE AT SERVER:(port,IP) = ("+ successor.getPort() + "," + successor.getIP() + ")" + " hashed at " + successor.getHashedValue();
        }
        else if (successor != null){
            reply = "CORRECT SERVER: hashed key of [" + MetadataUtils.hash(message.getKey()) + "] served by server (port,IP) = (" + successor.getPort() + "," + successor.getIP() + ")" + " hashed at " + successor.getHashedValue();
		// TODO: NEED TO RETURN HERE FOR A REROUTE IMMEDIATELY
        }

        switch (message.getCommand()) {
            case GET:
                String getValue = "";
                if (!reroute){
                    response = storageManager.get(message.getKey(), version);
                    
                    logger.info("RECEIVED GET REQUEST");
                    if (getValue != null && getValue.length() > 0) {
                        // GET SUCCESS
                        responseStatus = StatusType.GET_SUCCESS;
                    }
                    else {
                        // GET FAIL
                        responseStatus = StatusType.GET_ERROR;
                    }
                }
                response
                    .setKey(message.getKey())
                    .setStatus(responseStatus)
                    .setMetadata(metadata)
                    .setMessage(reply);
                break;
            
            case PUT:
                if (!reroute) {
                    logger.info("RECEIVED PUT REQUEST");
                    responseStatus = storageManager.set(message.getKey(), message.getValue(), version);
                }
                response
                    .setKey(message.getKey())
                    .setValue(message.getValue())
                    .setStatus(responseStatus)
                    .setMetadata(metadata)
                    .setMessage(reply);
                break;

            case DELETE:
                if (!reroute) {
                    logger.info("RECEIVED DELETE REQUEST");
                    responseStatus = storageManager.delete(message.getKey(), version); 
                }
                response
                    .setKey(message.getKey())
                    .setStatus(responseStatus)
                    .setMetadata(metadata)
                    .setMessage(reply);
            break;

            case CHAT:
                // why might they be sending us this?
                // if there is a message, print it
                if (message.getMessage().length() > 0) {
                    logger.info("CLIENT SENT MESSAGE: " + message.getMessage());
                }
                break;

            case INIT_CLIENT_METADATA:
                response
                    .setMetadata(server.getMetadata())
                    .setStatus(StatusType.SUCCESS);
                break;
        }
        logger.info("REPLIED TO " + message.getCommand() + " WITH STATUS " + response.getStatus());
        return response;
    }
}
