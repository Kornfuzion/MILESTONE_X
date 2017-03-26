package handlers;

import app_kvEcs.*;
import app_kvServer.*;
import common.*;
import common.messages.*;
import datastore.*;
import logger.*;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.log4j.*;

public class ClientHandler implements MessageHandler {
    private final ClientType type = ClientType.CLIENT;
    private static Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private StorageManager storageManager;
    private KVServer server;
    private static final int COORDINATOR = 1;

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

    public String formatServerIdentifier(int serverIdentifier) {
        return "_" + serverIdentifier;
    }

    private void forwardMessageToReplicas(KVMessage message) {
        try {
            // Repurpose the incoming message to forward to replicas
            message = message.setClientType(ClientType.COORDINATOR);
            for (Socket socket : server.getReplicaSockets()) {
                KVMessageUtils.sendMessage(message, socket.getOutputStream());
                KVMessageUtils.receiveMessage(socket.getInputStream());
            }
        } catch (Exception e) {
            // Please Jesus don't spite me for not handling exceptions properly
            e.printStackTrace();
        }
    }

    public KVMessage handleMessage(KVMessage message) throws Exception {
        KVServerStatus serverStatus = server.getServerStatus();
        CommandType command = message.getCommand();
		// If the server isn't supposed to be accepting user requests yet,
        // Block the request, reply with an ERROR message
        if (!server.isRunning() && server.alive()) {
           return new KVMessage(command)
                        .setStatus(StatusType.SERVER_STOPPED);
        }

        KVMessage response = new KVMessage(command);
        StatusType responseStatus = StatusType.ERROR;
        String reply = "";

        int version = 0;
		// Checking for if server is write locked.
		if (command == CommandType.PUT || command == CommandType.DELETE) {
        	serverStatus.writeReadLock();
        	// Server is under write lock, return write lock message to client.
        	if (serverStatus.isWriteLocked()) {
            	serverStatus.writeReadUnlock();
            	responseStatus = StatusType.SERVER_WRITE_LOCK;
            	response.setStatus(responseStatus)
                    	.setMessage(reply);
            	logger.info("REPLIED TO " + command + " WITH STATUS " + response.getStatus());
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

        int serverIdentifier = MetadataUtils.getServerIdentifier(
                                    server.getPort(), 
                                    successor, 
                                    server.getMetadata(),
                                    command);
        boolean isSuccessor = (command == CommandType.GET && serverIdentifier > 0) ||
                              (command != CommandType.GET && serverIdentifier == 1);
        boolean reroute = !isSuccessor || !server.alive();
		serverStatus.metadataReadUnlock();

        if (reroute) {
            responseStatus = StatusType.REROUTE;
            metadata = server.getMetadata();
            reply = "INCORRECT SERVER: hashed key of [" + MetadataUtils.hash(message.getKey()) + "] served by server (port,IP) = (" + server.getPort() + ",127.0.0.1)" + " BUT SHOULD BE AT SERVER:(port,IP) = ("+ successor.getPort() + "," + successor.getIP() + ")" + " hashed at " + successor.getHashedValue();
        }
        else if (successor != null){
            reply = "CORRECT SERVER: hashed key of [" + MetadataUtils.hash(message.getKey()) + "] served by server (port,IP) = (" + server.getPort() + ",127.0.0.1)";
		// TODO: NEED TO RETURN HERE FOR A REROUTE IMMEDIATELY
        }

        switch (command) {
            case GET:
                if (!reroute){
                    response = storageManager.get(message.getKey(), version, formatServerIdentifier(serverIdentifier));
                    
                    logger.info("RECEIVED GET REQUEST");
                response
                    .setKey(message.getKey())
                    .setMetadata(metadata)
                    .setMessage(reply);
                } else {
                    response
                    .setKey(message.getKey())
                    .setStatus(responseStatus)
                    .setMetadata(metadata)
                    .setMessage(reply);
                }
                break;
            case PUT:
                if (!reroute) {
                    logger.info("RECEIVED PUT REQUEST");
                    responseStatus = storageManager.set(message.getKey(), message.getValue(), version, formatServerIdentifier(serverIdentifier));
                    if (serverIdentifier == COORDINATOR) {
                        //forwardMessageToReplicas(message);
                    }
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
                    responseStatus = storageManager.delete(message.getKey(), version, formatServerIdentifier(serverIdentifier)); 
                    if (serverIdentifier == COORDINATOR) {
                        //forwardMessageToReplicas(message);
                    }
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
        logger.info("REPLIED TO " + command + " WITH STATUS " + response.getStatus());
        return response;
    }
}
