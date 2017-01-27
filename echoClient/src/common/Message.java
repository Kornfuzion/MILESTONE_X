package client;

import java.io.Serializable;
import java.io.StringWriter;
import java.io.*;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

/**
 * Represents a simple text message, which is intended to be received and sent 
 * by the server.
 */

public class Message {

    private Logger logger = Logger.getRootLogger();
	private static final char LINE_FEED = 0x0A;
	private static final char RETURN = 0x0D;
    
    // I know these JSON key names are a bit long, we can change them later
    // or figure out a cleaner way to list our JSON members
    public static String SEQUENCE_FIELD = "sequence_num";
    public static String COMMAND_FIELD = "command";
    public static String MESSAGE_FIELD = "message";

	private String msg;
	private byte[] msgBytes;
    private int sequenceNum;
    private CommandType command;
	
    /**
     * Constructs a Message object with a given array of bytes that 
     * forms the message.
     * 
     * @param bytes the bytes that form the message in ASCII coding.
     */
	public Message(byte[] bytes) {
		this.msgBytes = addCtrChars(bytes);
		parseBytesToMessage(new String(bytes));
	}

	/**
     * Constructs a Message object with a given String that
     * forms the message. 
     * 
     * @param msg the String that forms the message.
     */
	public Message(int sequenceNum, CommandType command, String msg) {
		this.msg = msg;
        this.sequenceNum = sequenceNum;
        this.command = command;
		this.msgBytes = toByteArray(this.sequenceNum, this.command, this.msg);
	}

	public Message(String msg) {
		this.msg = msg;
        this.sequenceNum = -1;
        this.command = CommandType.INVALID;
		this.msgBytes = toByteArray(this.sequenceNum, this.command, this.msg);
	}

	/**
	 * Returns the content of this Message as a String.
	 * 
	 * @return the content of this message in String format.
	 */
	public String getMsg() {
		return msg.trim();
	}

	/**
	 * Returns an array of bytes that represent the ASCII coded message content.
	 * 
	 * @return the content of this message as an array of bytes 
	 * 		in ASCII coding.
	 */
	public byte[] getMsgBytes() {
		return msgBytes;
	}
	
	private byte[] addCtrChars(byte[] bytes) {
		byte[] ctrBytes = new byte[]{RETURN};
		byte[] tmp = new byte[bytes.length + ctrBytes.length];
		
		System.arraycopy(bytes, 0, tmp, 0, bytes.length);
		System.arraycopy(ctrBytes, 0, tmp, bytes.length, ctrBytes.length);
		
		return tmp;		
	}
	
	private byte[] toByteArray(int sequenceNum, CommandType command, String msg) {
        try {
            JSONObject obj = new JSONObject();

            obj.put(SEQUENCE_FIELD, sequenceNum);
            obj.put(COMMAND_FIELD, command.ordinal());
            obj.put(MESSAGE_FIELD, msg);

            StringWriter out = new StringWriter();
            obj.writeJSONString(out);
            byte[] bytes = out.toString().getBytes("UTF-8");
		    byte[] ctrBytes = new byte[]{RETURN};
		    byte[] tmp = new byte[bytes.length + ctrBytes.length];
		
		    System.arraycopy(bytes, 0, tmp, 0, bytes.length);
		    System.arraycopy(ctrBytes, 0, tmp, bytes.length, ctrBytes.length);
		
		    return tmp;		
        } catch (IOException e) {
            logger.info("IO EXCEPTION: error during Message serialization");
            e.printStackTrace();

            return null;
        } 
	}

    private void parseBytesToMessage(String s) {
        try {
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(s);
            this.msg = (String) jsonObject.get(MESSAGE_FIELD);
            this.sequenceNum = Integer.valueOf(((Long) jsonObject.get(SEQUENCE_FIELD)).intValue());
            this.command = CommandType.values()
                [Integer.valueOf(((Long) jsonObject.get(COMMAND_FIELD)).intValue())];
        } catch (ParseException e) {
            logger.info("PARSE EXCEPTION: error parsing bytes to Message");
            e.printStackTrace();

            this.msg = null;
            this.sequenceNum = -1;
            this.command = CommandType.INVALID;

            return;
        }
    }
}
