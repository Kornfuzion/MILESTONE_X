package handlers;

import app_kvEcs.*;
import app_kvServer.*;
import common.*;
import common.messages.*;
import datastore.*;
import logger.*;

import java.io.*;
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

    public static String getStackTrace(final Throwable throwable) {
         final StringWriter sw = new StringWriter();
         final PrintWriter pw = new PrintWriter(sw, true);
         throwable.printStackTrace(pw);
         return sw.getBuffer().toString();
    }

    public ClientType getClientType() {
        return this.type;
    }

    public String formatServerIdentifier(int serverIdentifier) {
        return "_" + serverIdentifier;
    }

    public KVMessage handleMessage(KVMessage message) throws Exception {
        try {
        KVServerStatus serverStatus = server.getServerStatus();
        CommandType command = message.getCommand();
        KVMessage response = new KVMessage(command).setMessage("REPLICA SUCCESS");
        StatusType responseStatus = StatusType.ERROR;
        String reply = "WROTE SUCCESSFULLAY";

        int version = 0;
		// Checking for if server is write locked.

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
		serverStatus.metadataReadLock();

        ECSNode successor = MetadataUtils.getSuccessor(MetadataUtils.hash(message.getKey()), server.getMetadata());
        int serverIdentifier = MetadataUtils.getServerIdentifier(
                                    server.getPort(), 
                                    successor, 
                                    server.getMetadata(),
                                    command);

		serverStatus.metadataReadUnlock();

        switch (command) {
            case PUT:
                logger.info("RECEIVED PUT REQUEST");
                if (storageManager == null) {
                    logger.error("WTF DUDE WHY IS STORAGEMANAGER NULL");
                }
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
        logger.info("REPLIED TO " + command + " WITH STATUS " + response.getStatus());
        return response;
    } catch (Exception e) {
        logger.error(getStackTrace(e));
    }
        return null;
    }

}
