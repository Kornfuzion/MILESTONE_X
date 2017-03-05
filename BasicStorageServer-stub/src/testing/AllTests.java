package testing;

import java.io.IOException;

import org.apache.log4j.Level;

import app_kvServer.KVServer;
import junit.framework.Test;
import junit.framework.TestSuite;
import logger.LogSetup;


public class AllTests {

    static {
        try {
            new LogSetup("logs/testing/test.log", Level.ERROR);
            new KVServer(50000, 10, "FIFO").start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    public static Test suite() {
        TestSuite clientSuite = new TestSuite("Basic Storage ServerTest-Suite");

        //clientSuite.addTestSuite(ConnectionTest.class);
        //clientSuite.addTestSuite(InteractionTest.class); 
        clientSuite.addTestSuite(LruTest.class);
        clientSuite.addTestSuite(LfuTest.class);
        clientSuite.addTestSuite(FifoTest.class);
        clientSuite.addTestSuite(StorageManagerTest.class);
	clientSuite.addTestSuite(ECSClientTest.class);
        return clientSuite;
    }
    
}
