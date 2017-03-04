package common.messages;

import app_kvEcs.*;
import cache.*;
import common.messages.*;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.*;
import java.util.*;
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
    public static String STATUS_FIELD = "status";
    public static String CLIENT_TYPE_FIELD = "client_type";

    // Regular client fields
    public static String KEY_FIELD = "key";
    public static String VALUE_FIELD = "value";
    public static String MESSAGE_FIELD = "message";
    // ECS client fields
    public static String METADATA_FIELD = "metadata";
    public static String CACHE_SIZE_FIELD = "cache_size";
    public static String CACHE_POLICY_FIELD = "cache_policy";

	private byte[] serializedBytes;

    private CommandType command;
    private StatusType status;
    private ClientType clientType;
    // For regular clients
    private String key;
    private String value;
    private String message;
    // For ECS clients
    private TreeSet<ECSNode> metadata;
    private int cacheSize;
    private CachePolicy cachePolicy;
	
    /**
     * Constructs a Message object with a given array of bytes that 
     * forms the message.
     * 
     * @param bytes the bytes that form the message in ASCII coding.
     */
	public static KVMessage parse(byte[] bytes) throws Exception {
                KVMessage message = new KVMessage(null);
		message.serializedBytes = addCtrChars(bytes);
		message.parseBytesToMessage(new String(bytes));
                return message;   
	}

	/**
     * Constructs a Message object with a given String that
     * forms the message. 
     * 
     * @param msg the String that forms the message.
     */
	public KVMessage(CommandType command) {
        this.command = command;
        this.status = StatusType.INVALID;
        this.clientType = ClientType.INVALID;
        this.key = EMPTY_STRING;
        this.value = EMPTY_STRING;
        this.message = EMPTY_STRING;
        this.metadata = new TreeSet<ECSNode>(new hashRingComparator());
		this.cacheSize = 0;
		this.cachePolicy = CachePolicy.FIFO;
	}

    private String serializeMetadata(TreeSet<ECSNode> metadata) {
        String serial = "";
        // Kind of hacky, but let's just serialize it as PORT,IP|PORT,IP|PORT,IP|... 
        // in sorted order
        for (ECSNode node : metadata) {
            // Put the node in PORT,IP format
            String serialNode = String.format("%s,%s|", node.getPort(), node.getIP());
            serial = serial + serialNode;
        }
        return serial;
    }

    private void deserializeMetadata(String serial) {
        String[] serialNodes = serial.split("|");
        for (String serialNode : serialNodes) {
            String[] nodeInfo = serialNode.split(",");
			if (nodeInfo.length >= 2) {
		        String port = nodeInfo[0];
		        String IP = nodeInfo[1];
				System.out.println(nodeInfo[0] + " " + nodeInfo[1]);
		        metadata.add(new ECSNode(port, IP));
			}
        }
    }

	/**
	 * 
	 * @return the content of this object in JSON format as an array of bytes 
	 * 		in ASCII coding.
	 */
	public byte[] getSerializedBytes() {
        if (this.serializedBytes == null) {
            this.serializedBytes = toByteArray();
        }
		return serializedBytes;
	}
	
	private static byte[] addCtrChars(byte[] bytes) {
		byte[] ctrBytes = new byte[]{RETURN};
		byte[] tmp = new byte[bytes.length + ctrBytes.length];
		
		System.arraycopy(bytes, 0, tmp, 0, bytes.length);
		System.arraycopy(ctrBytes, 0, tmp, bytes.length, ctrBytes.length);
		
		return tmp;		
	}
	
	private byte[] toByteArray() {
        try {
            JSONObject obj = new JSONObject();

            obj.put(COMMAND_FIELD, command.ordinal());
            obj.put(KEY_FIELD, key);
            obj.put(VALUE_FIELD, value);
            obj.put(MESSAGE_FIELD, message);
            obj.put(STATUS_FIELD, status.ordinal());
            obj.put(CLIENT_TYPE_FIELD, clientType.ordinal());
            obj.put(METADATA_FIELD, serializeMetadata(metadata));
            obj.put(CACHE_SIZE_FIELD, cacheSize);
            obj.put(CACHE_POLICY_FIELD, cachePolicy.ordinal());

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

    private void parseBytesToMessage(String s) throws Exception {
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(s);
            this.command = CommandType.values()
                [Integer.valueOf(((Long) jsonObject.get(COMMAND_FIELD)).intValue())];
            this.key = (String) jsonObject.get(KEY_FIELD);
            this.value = (String) jsonObject.get(VALUE_FIELD);
            this.message = (String) jsonObject.get(MESSAGE_FIELD);
            this.status = StatusType.values()
                [Integer.valueOf(((Long) jsonObject.get(STATUS_FIELD)).intValue())];
            this.clientType = ClientType.values()
                [Integer.valueOf(((Long) jsonObject.get(CLIENT_TYPE_FIELD)).intValue())];
            deserializeMetadata((String) jsonObject.get(METADATA_FIELD));
            this.cacheSize = Integer.valueOf(((Long) jsonObject.get(CACHE_SIZE_FIELD)).intValue());
            this.cachePolicy = CachePolicy.values()
                [Integer.valueOf(((Long) jsonObject.get(CACHE_POLICY_FIELD)).intValue())];
    } 
 
    // Create request methods
    public static KVMessage createGetRequest(String key) {
        return new KVMessage(CommandType.GET)
                    .setKey(key)
                    .setValue(EMPTY_STRING)
                    .setMessage(EMPTY_STRING)
                    .setStatus(StatusType.INVALID)
                    .setClientType(ClientType.INVALID);
    }

    public static KVMessage createPutRequest(String key, String value) {
        return new KVMessage(CommandType.PUT)
                    .setKey(key)
                    .setValue(value)
                    .setMessage(EMPTY_STRING)
                    .setStatus(StatusType.INVALID)
                    .setClientType(ClientType.CLIENT);
    }

    public static KVMessage createDeleteRequest(String key) {
        return new KVMessage(CommandType.DELETE)
                    .setKey(key)
                    .setValue(EMPTY_STRING)
                    .setMessage(EMPTY_STRING)
                    .setStatus(StatusType.INVALID)
                    .setClientType(ClientType.CLIENT);
    }

    public static KVMessage createChatMessage(String message) {
        return new KVMessage(CommandType.CHAT)
                    .setKey(EMPTY_STRING)
                    .setValue(EMPTY_STRING)
                    .setMessage(message)
                    .setStatus(StatusType.SUCCESS)
                    .setClientType(ClientType.CLIENT);
    }

    // Getter methods
    public CommandType getCommand() {
        return this.command;
    }

    public String getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }   

    public String getMessage() {
        return this.message;
    }   

    public StatusType getStatus() {
        return this.status;
    }

    public ClientType getClientType() {
        return this.clientType;
    }
    
    public TreeSet<ECSNode> getMetadata() {
        return this.metadata;
    }

    public int getCacheSize() {
        return this.cacheSize;
    }

    public CachePolicy getCachePolicy() {
        return this.cachePolicy;
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

    public KVMessage setClientType(ClientType clientType) {
        this.clientType = clientType;
        return this;
    }

    public KVMessage setMetadata(TreeSet<ECSNode> metadata) {
        this.metadata = metadata;
        return this;
    }

    public KVMessage setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
        return this;
    }

    public KVMessage setCachePolicy(CachePolicy cachePolicy) {
        this.cachePolicy = cachePolicy;
        return this;
    }
}
