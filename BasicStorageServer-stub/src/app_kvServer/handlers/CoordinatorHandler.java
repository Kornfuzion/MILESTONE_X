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

public class CoordinatorHandler implements MessageHandler {
    private final ClientType type = ClientType.COORDINATOR;
    private static Logger logger = Logger.getLogger(CoordinatorHandler.class.getName());
    private StorageManager storageManager;
    private KVServer server;

    public CoordinatorHandler(KVServer server, StorageManager storageManager) {
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

    public KVMessage handleMessage(KVMessage message) throws Exception {
        KVServerStatus serverStatus = server.getServerStatus();
        KVMessage response = new KVMessage(message.getCommand());
        StatusType responseStatus = StatusType.ERROR;
        String reply = "";

        int version = 0;
		// Checking for if server is write locked.

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
		serverStatus.metadataReadLock();

        ECSNode successor = MetadataUtils.getSuccessor(MetadataUtils.hash(message.getKey()), server.getMetadata());
        int serverIdentifier = server.isSuccessor(successor, message.getCommand());

		serverStatus.metadataReadUnlock();

        switch (message.getCommand()) {
            case PUT:
                logger.info("RECEIVED PUT REQUEST");
                responseStatus = storageManager.set(message.getKey(), message.getValue(), version, formatServerIdentifier(serverIdentifier));
                response
                    .setKey(message.getKey())
                    .setValue(message.getValue())
                    .setStatus(responseStatus)
                    .setMessage(reply);
                break;

            case DELETE:
                logger.info("RECEIVED DELETE REQUEST");
                responseStatus = storageManager.delete(message.getKey(), version, formatServerIdentifier(serverIdentifier)); 
                response
                    .setKey(message.getKey())
                    .setStatus(responseStatus)
                    .setMessage(reply);
            break;
        }
        logger.info("REPLIED TO " + message.getCommand() + " WITH STATUS " + response.getStatus());
        return response;
    }
}
