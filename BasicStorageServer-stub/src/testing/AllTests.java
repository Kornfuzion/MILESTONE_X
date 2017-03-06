package testing;

import java.io.IOException;

import org.apache.log4j.Level;

import app_kvServer.KVServer;
import junit.framework.Test;
import junit.framework.TestSuite;
import logger.LogSetup;
import app_kvEcs.*;
import java.util.*;

public class AllTests {

    static {
        try {
            Comparator<ECSNode> comparator = new hashRingComparator();
            TreeSet<ECSNode> metadata = new TreeSet<ECSNode>(comparator);
	        metadata.add(new ECSNode("50000","0"));
            metadata.add(new ECSNode("50001", "0"));	
            new LogSetup("logs/testing/test.log", Level.ERROR);
            new KVServer(50000, 10, "FIFO", metadata).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    public static Test suite() {
        TestSuite clientSuite = new TestSuite("Basic Storage ServerTest-Suite");

        clientSuite.addTestSuite(ConnectionTest.class);
        clientSuite.addTestSuite(InteractionTest.class); 
        clientSuite.addTestSuite(MessageTest.class);
        clientSuite.addTestSuite(LruTest.class);
        clientSuite.addTestSuite(LfuTest.class);
        clientSuite.addTestSuite(FifoTest.class);
        clientSuite.addTestSuite(StorageManagerTest.class);
	    clientSuite.addTestSuite(ECSClientTest.class);
        return clientSuite;
    }
    
}
