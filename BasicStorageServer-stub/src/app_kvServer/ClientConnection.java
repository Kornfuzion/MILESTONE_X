package app_kvServer;

import common.messages.*;
import common.messages.status.*;
import common.messages.commands.*;

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
public class ClientConnection implements Runnable {

    private static Logger logger = Logger.getRootLogger();
    
    private boolean isOpen;
    private static final int BUFFER_SIZE = 1024;
    private static final int DROP_SIZE = 128 * BUFFER_SIZE;
    
    private Socket clientSocket;
    private InputStream input;
    private OutputStream output;
    private StorageManager storageManager;
    
    /**
     * Constructs a new CientConnection object for a given TCP socket.
     * @param clientSocket the Socket object for the client connection.
     */
    public ClientConnection(Socket clientSocket, StorageManager storageManager) {
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

            sendMessage(KVMessage.createConnectionResponse(
                    "Connection to MSRG Echo server established: " 
                    + clientSocket.getLocalAddress() + " / "
                    + clientSocket.getLocalPort()));
            
            while(isOpen) {
                try {
                    handleRequest(receiveMessage());
                /* connection either terminated by the client or lost due to 
                 * network problems*/   
                } catch (Exception e) {
                    logger.error("Error! Connection lost!");
                    isOpen = false;
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
    
    /**
     * Method sends a Message using this socket.
     * @param msg the message that is to be sent.
     * @throws IOException some I/O error regarding the output stream 
     */
    public void sendMessage(KVMessage msg) throws IOException {
        byte[] msgBytes = msg.getSerializedBytes();
        output.write(msgBytes, 0, msgBytes.length);
        output.flush();
        logger.info("SEND \t<" 
                + clientSocket.getInetAddress().getHostAddress() + ":" 
                + clientSocket.getPort() + ">: '" 
                +"'");
    }

    private void handleRequest(KVMessage message) throws Exception {
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
        if (response != null) {
            sendMessage(response);
        }
    }
    
    private KVMessage receiveMessage() throws Exception {
        
        int index = 0;
        byte[] msgBytes = null, tmp = null;
        byte[] bufferBytes = new byte[BUFFER_SIZE];
        
        /* read first char from stream */
        byte read = (byte) input.read();    
        boolean reading = true;
        
//      logger.info("First Char: " + read);
//      Check if stream is closed (read returns -1)
//      if (read == -1){
//          TextMessage msg = new TextMessage("");
//          return msg;
//      }

        while (read != 13 && reading) {/* CR, LF, error */
            /* if buffer filled, copy to msg array */
            if(index == BUFFER_SIZE) {
                if(msgBytes == null){
                    tmp = new byte[BUFFER_SIZE];
                    System.arraycopy(bufferBytes, 0, tmp, 0, BUFFER_SIZE);
                } else {
                    tmp = new byte[msgBytes.length + BUFFER_SIZE];
                    System.arraycopy(msgBytes, 0, tmp, 0, msgBytes.length);
                    System.arraycopy(bufferBytes, 0, tmp, msgBytes.length,
                            BUFFER_SIZE);
                }

                msgBytes = tmp;
                bufferBytes = new byte[BUFFER_SIZE];
                index = 0;
            } 
            
            /* only read valid characters, i.e. letters and constants */
            bufferBytes[index] = read;
            index++;
            
            /* stop reading is DROP_SIZE is reached */
            if(msgBytes != null && msgBytes.length + index >= DROP_SIZE) {
                reading = false;
            }
            
            /* read next char from stream */
            read = (byte) input.read();
        }
        
        if(msgBytes == null){
            tmp = new byte[index];
            System.arraycopy(bufferBytes, 0, tmp, 0, index);
        } else {
            tmp = new byte[msgBytes.length + index];
            System.arraycopy(msgBytes, 0, tmp, 0, msgBytes.length);
            System.arraycopy(bufferBytes, 0, tmp, msgBytes.length, index);
        }
        
        msgBytes = tmp;
        
        /* build final String */
        KVMessage msg = KVMessage.parse(msgBytes);
        logger.info("RECEIVE \t<" 
                + clientSocket.getInetAddress().getHostAddress() + ":" 
                + clientSocket.getPort() + ">: '" 
                + msg.getMessage().trim() + "'");
        return msg;
    }
    

    
}
