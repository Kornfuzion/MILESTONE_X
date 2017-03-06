package testing;

import app_kvEcs.*;
import common.messages.*;
import common.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.ClassLoader;
import java.util.*;

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
        TreeSet<ECSNode> metadata = new TreeSet<ECSNode>();
        Map<String,Integer> countNodes = new HashMap<String,Integer>();
        metadata.add(new ECSNode("5000", "127.0.0.1"));
        metadata.add(new ECSNode("5001", "127.0.0.1"));
        for (ECSNode node : metadata) {
            countNodes.put(node.getHashedValue(), 1);
        }
        
        message
            .setKey(key)
            .setValue(value)
            .setStatus(status)
            .setMetadata(metadata);

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
        TreeSet<ECSNode> receivedMetadata = received.getMetadata();
        for (ECSNode node : receivedMetadata) {
            int seen = countNodes.get(node.getHashedValue());
            countNodes.put(node.getHashedValue(), seen - 1);
        }
        
        int receivedNodes = 0;
        for (int count : countNodes.values()) {
            receivedNodes += count;
        }
        assert(receivedNodes == 0);
    }
}
