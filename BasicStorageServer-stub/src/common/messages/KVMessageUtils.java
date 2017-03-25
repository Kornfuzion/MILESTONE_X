package common.messages;

import common.messages.*;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.*;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;


/**
 * Represents a simple text message, which is intended to be received and sent 
 * by the server.
 */

public class KVMessageUtils {
    public static final int BUFFER_SIZE = 1024;
    public static final int DROP_SIZE = 128 * BUFFER_SIZE;


    /**
     * Method sends a Message using this socket.
     * @param msg the message that is to be sent.
     * @throws IOException some I/O error regarding the output stream 
     */
    public static void sendMessage(KVMessage msg, OutputStream output) throws IOException {
        byte[] msgBytes = msg.getSerializedBytes();
        output.write(msgBytes, 0, msgBytes.length);
        output.flush();
    }
    
    public static KVMessage receiveMessage(InputStream input) throws Exception {
        
        int index = 0;
        byte[] msgBytes = null, tmp = null;
        byte[] bufferBytes = new byte[BUFFER_SIZE];
        
        /* read first char from stream */
        byte read = (byte) input.read();    
        boolean reading = true;

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
        return KVMessage.parse(msgBytes);
    }

    public static void sendReceiveMessage(CommandType commandType, Socket socket) throws Exception {
        sendReceiveMessage(commandType, socket, true);
    }

    public static void sendReceiveMessage(CommandType commandType, Socket socket, boolean printResponse) throws Exception {
        KVMessage message = new KVMessage(commandType)
                            .setClientType(ClientType.ECS);  
        KVMessageUtils.sendMessage(message, socket.getOutputStream());
        KVMessage receiveMessage = KVMessageUtils.receiveMessage(socket.getInputStream());
        
        if (printResponse) {
            System.out.println(receiveMessage.getCommand() + " " + receiveMessage.getStatus());
        }
    }
}
