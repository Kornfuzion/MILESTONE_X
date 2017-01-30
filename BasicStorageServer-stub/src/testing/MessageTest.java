package testing;

import common.messages.*;
import common.messages.commands.*;
import common.messages.status.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.ClassLoader;

import junit.framework.TestCase;


public class MessageTest extends TestCase {
    private KVMessage message;

    @Before
    public void setUp() {
        message = new KVMessage(CommandType.GET); 
    }
	
    @After
    public void tearDown() {
        //empty
    }
	
    @Test
    public void testSerialization(){
        String key = "TESTKEY";
	String value = "TESTVALUE";
        StatusType status = StatusType.GET_SUCCESS;       
        message
            .setKey(key)
            .setValue(value)
            .setStatus(status);

       	byte[] serialized = message.getSerializedBytes();

        // we need to remove the 1 control character terminating this byte array
        byte[] truncated = new byte[serialized.length - 1];
       
        for (int i = 0; i < serialized.length - 1; i++) {
            truncated[i] = serialized[i];
        }
        KVMessage received = null;
        try {
            received = KVMessage.parse(truncated);
        }
        catch (Exception e) {
            assert(false);

        }
        assert(received != null); 
        assert(received.getCommand() == CommandType.GET);
        assert(received.getKey() == key);
        assert(received.getValue() == value);
        assert(received.getStatus() == status);         
    }
}
