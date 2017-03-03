package app_kvServer;

import common.messages.*;

import datastore.*;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.log4j.*;

/**
 * Represents a connection end point for a particular client that is 
 * connected to the server. This class is responsible for message reception 
 * and sending. 
 * The class also implements the echo functionality. Thus whenever a message 
 * is received it is going to be echoed back to the client.
 */
public class RequestConnection implements Runnable {

    private static Logger logger = Logger.getRootLogger();
    
    private boolean isOpen;
    private static final int BUFFER_SIZE = 1024;
    private static final int DROP_SIZE = 128 * BUFFER_SIZE;
    
    private Socket clientSocket;
    private InputStream input;
    private OutputStream output;
    private StorageManager storageManager;
    
    /**
     * Constructs a new RequestConnection object for a given TCP socket.
     * @param clientSocket the Socket object for the client connection.
     */
    public RequestConnection(Socket clientSocket, StorageManager storageManager /*, Server state for ECS */) {
        this.clientSocket = clientSocket;
        this.storageManager = storageManager;
        this.isOpen = true;
    }
    
    /**
     * Initializes and starts the client connection. 
     * Loops until the connection is closed or aborted by the client.
     */
    public void run() {
        try {
            output = clientSocket.getOutputStream();
            input = clientSocket.getInputStream();

            KVMessage responseMessage = KVMessage.createChatMessage(
                    "Connection to MSRG Echo server established: " 
                    + clientSocket.getLocalAddress() + " / "
                    + clientSocket.getLocalPort());
            KVMessageUtils.sendMessage(responseMessage, output);
            
            // This is now a one-time connection, so just close the socket after we're done
            try {
                KVMessage receivedMessage = KVMessageUtils.receiveMessage(input);
                
            /* connection either terminated by the client or lost due to 
             * network problems*/   
            } catch (Exception e) {
               logger.error("Error! Connection lost!");
               isOpen = false;
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
