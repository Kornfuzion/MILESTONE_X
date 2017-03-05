package app_kvServer;

import common.messages.*;
import datastore.*;
import handlers.*;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import logger.*;
import java.net.Socket;
import java.util.*;

import org.apache.log4j.*;

/**
 * Represents a connection end point for a particular client that is 
 * connected to the server. This class is responsible for message reception 
 * and sending. 
 * The class also implements the echo functionality. Thus whenever a message 
 * is received it is going to be echoed back to the client.
 */
public class RequestConnection implements Runnable {

    private static Logger logger = Logger.getLogger(RequestConnection.class.getName());
    
    private Socket clientSocket;
    private InputStream input;
    private OutputStream output;
    private StorageManager storageManager;
    private KVServer server;

    private ArrayList<MessageHandler> messageHandlers;
    
    /**
     * Constructs a new RequestConnection object for a given TCP socket.
     * @param clientSocket the Socket object for the client connection.
     */
    public RequestConnection(KVServer server, Socket clientSocket, StorageManager storageManager) {
        this.server = server;
        this.clientSocket = clientSocket;
        this.storageManager = storageManager;
        this.messageHandlers = new ArrayList<MessageHandler>();
        addMessageHandlers();
        try {
            new LogSetup("logs/server/server.log", Level.ALL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void addMessageHandlers() {
        messageHandlers.add(new ClientHandler(server, storageManager));
        messageHandlers.add(new ECSHandler(this, server));
    }

    /**
     * Initializes and starts the client connection. 
     * Loops until the connection is closed or aborted by the client.
     */
    public void run() {
        try {
            output = clientSocket.getOutputStream();
            input = clientSocket.getInputStream();       
    
            // Keep connections open, let the client close connections when metadata is stale
            while (true) {
                try {
                    KVMessage receivedMessage = KVMessageUtils.receiveMessage(input);
                    logger.info("SERVING REQUEST " + receivedMessage.getCommand());
                    KVMessage reply = null;
                    
                    for (MessageHandler handler : messageHandlers) {
                        if (receivedMessage.getClientType() == handler.getClientType()) {
                            reply = handler.handleMessage(receivedMessage);
                        }
                    }
                        
                    if (reply != null) {
                        logger.info(reply.getCommand() + " " + reply.getStatus());
                        KVMessageUtils.sendMessage(reply, output);
                    }
                /* connection either terminated by the client or lost due to 
                 * network problems*/   
                } catch (Exception e) {
                   logger.error("Error! Connection lost!");
		   //e.printStackTrace();	
                   break;
                }               
            }
        } catch (IOException ioe) {
            logger.error("Error! Connection could not be established!", ioe);
            
        } finally {
            
            try {
                if (clientSocket != null) {
                    input.close();
                    output.close();
                    clientSocket.close();
                }       
            } catch (IOException ioe) {
                logger.error("Error! Unable to tear down connection!", ioe);
            }
        }
    }
}
