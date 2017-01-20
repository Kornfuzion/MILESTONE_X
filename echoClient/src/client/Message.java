package client;

import java.io.Serializable;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

/**
 * Represents a simple text message, which is intended to be received and sent 
 * by the server.
 */
public class Message {

    public static String SEQUENCE_FIELD = "sequence_num";
    public static String COMMAND_FIELD = "command";
    public static String MESSAGE_FIELD = "message";
	private String msg;
	private byte[] msgBytes;
    private int sequenceNum;
    private int command;
	
    /**
     * Constructs a Message object with a given array of bytes that 
     * forms the message.
     * 
     * @param bytes the bytes that form the message in ASCII coding.
     */
	public Message(byte[] bytes) {
		this.msgBytes = addCtrChars(bytes);
		parseBytesToMessage(new String(this.msgBytes));
	}

	/**
     * Constructs a Message object with a given String that
     * forms the message. 
     * 
     * @param msg the String that forms the message.
     */
	public Message(int sequenceNum, int command, String msg) {
		this.msg = msg;
		this.msgBytes = toByteArray(msg);
        this.sequenceNum = sequenceNum;
        this.command = command;
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
		byte[] ctrBytes = new byte[]{LINE_FEED, RETURN};
		byte[] tmp = new byte[bytes.length + ctrBytes.length];
		
		System.arraycopy(bytes, 0, tmp, 0, bytes.length);
		System.arraycopy(ctrBytes, 0, tmp, bytes.length, ctrBytes.length);
		
		return tmp;		
	}
	
	private byte[] toByteArray(String s) {
        JSONObject obj = new JSONObject();

        obj.put(SEQUENCE_FIELD, sequenceNum);
        obj.put(COMMAND_FIELD, command);
        obj.put(MESSAGE_FIELD, msg);

        StringWriter out = new StringWriter();
        obj.writeJSONString(out);
      
        return out.toString().getBytes("UTF-8");
	}

    private void parseBytesToMessage(String s) {
        JSONObject jsonObject = new JSONObject(s);
        //TODO: PARSE THE MEMBER VARS BACK OUT
    }
}
