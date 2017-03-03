package handlers;

import app_kvServer.*;
import common.messages.*;
import datastore.*;

import java.io.IOException;
import org.apache.log4j.*;

public class ClientHandler implements MessageHandler {
    private final ClientType type = ClientType.CLIENT;
    private static Logger logger = Logger.getRootLogger();
    private StorageManager storageManager;
    private KVServer server;

    public ClientHandler(KVServer server, StorageManager storageManager) {
        this.server = server;
        this.storageManager = storageManager;
    }

    public ClientType getClientType() {
        return this.type;
    }

    public KVMessage handleMessage(KVMessage message) throws Exception {
        // If the server isn't supposed to be accepting user requests yet,
        // Block the request, reply with a NOT-READY message
        if (!server.isRunning()) {
           return new KVMessage(message.getCommand())
                        .setStatus(StatusType.ERROR);
        }

        KVMessage response = null;
        StatusType responseStatus;
        switch (message.getCommand()) {
            case GET:
                String getValue = storageManager.get(message.getKey());
                logger.info("RECEIVED GET REQUEST");
                if (getValue != null && getValue.length() > 0) {
                    // GET SUCCESS
                    responseStatus = StatusType.GET_SUCCESS;
                }
                else {
                    // GET FAIL
                    responseStatus = StatusType.GET_ERROR;
                }
                response = new KVMessage(CommandType.GET)
                                .setKey(message.getKey())
                                .setValue(getValue)
                                .setStatus(responseStatus);
                break;
            
            case PUT:
                logger.info("RECEIVED PUT REQUEST");
                responseStatus = storageManager.set(message.getKey(), message.getValue());
                response = new KVMessage(CommandType.PUT)
                                .setKey(message.getKey())
                                .setValue(message.getValue())
                                .setStatus(responseStatus);
                break;

            case DELETE:
                logger.info("RECEIVED DELETE REQUEST");
                responseStatus = storageManager.delete(message.getKey()); 
                response = new KVMessage(CommandType.DELETE)
                                .setKey(message.getKey())
                                .setStatus(responseStatus);
            break;

            case CHAT:
                // why might they be sending us this?
                // if there is a message, print it
                if (message.getMessage().length() > 0) {
                    logger.info("CLIENT SENT MESSAGE: " + message.getMessage());
                }
                break;
        }
        return response;
    }
}
