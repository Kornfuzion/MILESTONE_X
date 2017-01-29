package common.messages;

import common.messages.*;
import common.messages.commands.*;
import common.messages.status.*;
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

public class KVMessage {
    private Logger logger = Logger.getRootLogger();

    // Handy characters for this class
	public static final char LINE_FEED = 0x0A;
	public static final char RETURN = 0x0D;
    public static final String EMPTY_STRING = "";
    
    // I know these JSON key names are a bit long, we can change them later
    // or figure out a cleaner way to list our JSON members
    public static String COMMAND_FIELD = "command";
    public static String KEY_FIELD = "key";
    public static String VALUE_FIELD = "value";
    public static String STATUS_FIELD = "status";
    public static String MESSAGE_FIELD = "message";

	private byte[] serializedBytes;
    private CommandType command;
    private String key;
    private String value;
    private String message;
    private StatusType status;
	
    /**
     * Constructs a Message object with a given array of bytes that 
     * forms the message.
     * 
     * @param bytes the bytes that form the message in ASCII coding.
     */
	public KVMessage(byte[] bytes) {
		this.serializedBytes = addCtrChars(bytes);
		parseBytesToMessage(new String(bytes));
	}

	/**
     * Constructs a Message object with a given String that
     * forms the message. 
     * 
     * @param msg the String that forms the message.
     */
	public KVMessage(CommandType command) {
        this.command = command;
        this.key = EMPTY_STRING;
        this.value = EMPTY_STRING;
        this.message = EMPTY_STRING;
        this.status = StatusType.INVALID;
	}

	/**
	 * 
	 * @return the content of this object in JSON format as an array of bytes 
	 * 		in ASCII coding.
	 */
	public byte[] getSerializedBytes() {
        if (this.serializedBytes == null) {
            this.serializedBytes = toByteArray(this.command, this.key, this.value, this.message, this.status);
        }
		return serializedBytes;
	}
	
	private byte[] addCtrChars(byte[] bytes) {
		byte[] ctrBytes = new byte[]{RETURN};
		byte[] tmp = new byte[bytes.length + ctrBytes.length];
		
		System.arraycopy(bytes, 0, tmp, 0, bytes.length);
		System.arraycopy(ctrBytes, 0, tmp, bytes.length, ctrBytes.length);
		
		return tmp;		
	}
	
	private byte[] toByteArray(CommandType command, String key, String value, String message, StatusType status) {
        try {
            JSONObject obj = new JSONObject();

            obj.put(COMMAND_FIELD, command.ordinal());
            obj.put(KEY_FIELD, key);
            obj.put(VALUE_FIELD, value);
            obj.put(MESSAGE_FIELD, message);
            obj.put(STATUS_FIELD, status.ordinal());

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
            this.command = CommandType.values()
                [Integer.valueOf(((Long) jsonObject.get(COMMAND_FIELD)).intValue())];
            this.key = (String) jsonObject.get(KEY_FIELD);
            this.value = (String) jsonObject.get(VALUE_FIELD);
            this.message = (String) jsonObject.get(MESSAGE_FIELD);
            this.status = StatusType.values()
                [Integer.valueOf(((Long) jsonObject.get(STATUS_FIELD)).intValue())];
        } catch (ParseException e) {
            logger.info("PARSE EXCEPTION: error parsing bytes to Message");
            e.printStackTrace();
            this.command = CommandType.CHAT;
            this.key = EMPTY_STRING;
            this.value = EMPTY_STRING;
            this.message = EMPTY_STRING;
            this.status = StatusType.INVALID;

            return;
        }
    }

    // Create request methods
    public static KVMessage createGetRequest(String key) {
        return new KVMessage(CommandType.GET)
                    .setKey(key)
                    .setValue(EMPTY_STRING)
                    .setMessage(EMPTY_STRING)
                    .setStatus(StatusType.INVALID);
    }

    public static KVMessage createPutRequest(String key, String value) {
        return new KVMessage(CommandType.PUT)
                    .setKey(key)
                    .setValue(value)
                    .setMessage(EMPTY_STRING)
                    .setStatus(StatusType.INVALID);
    }

    public static KVMessage createDeleteRequest(String key) {
        return new KVMessage(CommandType.DELETE)
                    .setKey(key)
                    .setValue(EMPTY_STRING)
                    .setMessage(EMPTY_STRING)
                    .setStatus(StatusType.INVALID);
    }

    public static KVMessage createConnectionResponse(String message) {
        return new KVMessage(CommandType.CHAT)
                    .setKey(EMPTY_STRING)
                    .setValue(EMPTY_STRING)
                    .setMessage(message)
                    .setStatus(StatusType.SUCCESS);
    }

    // Getter methods
    public CommandType getCommand() {
        return command;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }   

    public String getMessage() {
        return message;
    }   

    public StatusType getStatus() {
        return status;
    }

    // Setter methods
    // TODO: input validation
    public KVMessage setCommand(CommandType command) {
        this.command = command;
        return this;
    }

    public KVMessage setKey(String key) {
        this.key = key;
        return this;
    }

    public KVMessage setValue(String value) {
        this.value = value;
        return this;
    }

    public KVMessage setMessage(String message) {
        this.message = message;
        return this;
    }

    public KVMessage setStatus(StatusType status) {
        this.status = status;
        return this;
    }
}
