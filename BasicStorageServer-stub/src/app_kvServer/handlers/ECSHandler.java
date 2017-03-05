package handlers;

import app_kvServer.*;
import app_kvEcs.*;
import cache.*;
import common.messages.*;
import logger.*;

import org.apache.log4j.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ECSHandler implements MessageHandler {
    private final ClientType type = ClientType.ECS;
    private static Logger logger = Logger.getLogger(ECSHandler.class.getName());
    private RequestConnection requestConnection;
    private KVServer server;

    public ECSHandler(RequestConnection requestConnection, KVServer server) {
        this.requestConnection = requestConnection;
        this.server = server;
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
        KVMessage reply = new KVMessage(message.getCommand())
                                .setStatus(StatusType.SUCCESS);
        switch (message.getCommand()) {
            case INIT:
                server.initKVServer(message.getMetadata(), message.getCacheSize(), message.getCachePolicy());
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
                serverStatus.writeWriteLock();
                // Change server status to be write locked.
                serverStatus.setWriteLocked(true);
				serverStatus.versionWriteLock();
                // Change server version
                serverStatus.updateVersion();
                serverStatus.versionWriteUnlock();
                serverStatus.writeWriteUnlock();

                server.blockStorageWrites();
                // At this point we are guaranteed that there will be no further writes. 
                break;
            case UNLOCK_WRITE:
                serverStatus.writeWriteLock();
                serverStatus.setWriteLocked(false);
                serverStatus.writeWriteUnlock();
                break;
            case MOVE_DATA:
                server.blockStorageWrites();
                //server.moveData();
                break;
            case UPDATE_METADATA:
				serverStatus.metadataWriteLock();
                server.updateMetadata(message.getMetadata());
				serverStatus.metadataWriteUnlock();
                break;
			case LOCK_WRITE_UPDATE_METADATA:
				server.blockStorageRerouteReads(message.getMetadata());
				break;
        }

        // For now, assume no failures possible so just return a generic success
        // Especially since the server will simply die if it fails to execute 
        // For example, in the case of initKVServer
        logger.info("REPLIED TO ECS CLIENT COMMAND [" + message.getCommand() + "] WITH STATUS [" + StatusType.SUCCESS + "]");
        return reply;

    }    
}
