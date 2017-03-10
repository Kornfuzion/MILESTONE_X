package testing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.ClassLoader;

import junit.framework.TestCase;
import app_kvEcs.*;
import app_kvClient.ui.*;
import common.messages.*;

public class ECSClientTest extends TestCase {
	private ECSClient client;
    private Application app;

	@Before
    public void setUp() {
		//put here
		client = new ECSClient();
	}
	
	@After
    public void tearDown() throws Exception{
        Process p = Runtime.getRuntime().exec("./killstuff.sh");
	    p.waitFor();
        // Wait for OS to free the socket.
        Thread.sleep(3000);
    }
	
	@Test
	public void testParseConfig(){
		Exception ex = null;
		try { 
			client.runConfig("./testECSClient2.config");
		} catch (Exception e) {
			ex = e;
			ex.printStackTrace();			
		}
		
		assertNull(ex);
		for(ECSNode n : client.getAvailableMachines()){
			int in = Integer.parseInt(n.getPort());
			if(in == 4000) {
				assertTrue(true);
			} else {
				assertTrue(false);			
			}
		}
	}
	
    /**
    Tests if the storage servers are initially in the start state right after
    initialization.
    */
    @Test
    public void testInit() throws Exception {
        client.initKVService(1, 100, "FIFO", "./testECSClient2.config");
        try {
		    Thread.sleep(2000);
		} catch (InterruptedException e) {
		    System.out.println(e);
		}
        app = new Application(true);
        KVMessage message =  app.getServerStatus();
        assertNotNull(message);
        assertTrue(message.getStatus() == StatusType.SERVER_STOPPED);
    }
 
    /**
    Tests if all the storage servers are in the stopped state after
    being stopped
    */
    @Test
	public void testStart() throws Exception {
        client.initKVService(1, 100, "FIFO", "./testECSClient2.config");
        // Start the storage servers
        try {
		    Thread.sleep(2000);
		} catch (InterruptedException e) {
		    System.out.println(e);
		}
        client.start();
        app = new Application(true);
        // Try to put a key-value pair into the storage server
		KVMessage message = app.put("randomkey", "randomvalue");
        assertNotNull(message);
        assertTrue(message.getStatus() != StatusType.SERVER_STOPPED);
	}

    /** 
    Tests if ECS can properly stop all initialized servers.
    */	
    @Test
	public void testStop() throws Exception{
        client.initKVService(1, 100, "FIFO", "./testECSClient2.config");
       // Start the storage servers
        try {
		    Thread.sleep(2000);
		} catch (InterruptedException e) {
		    System.out.println(e);
		}
        client.start();
        app = new Application(true);
        client.stop();
        KVMessage message =  app.getServerStatus();
        assertNotNull(message);
        assertTrue(message.getStatus() == StatusType.SERVER_STOPPED);
	}

    /**
    Tests if the ECS can add a node.
    */
	@Test
	public void testAddNode() {
        client.initKVService(1, 100, "FIFO", "./testECSClient3.config");
        assertTrue(client.getHashRingSize() == 1);
        try {
		    Thread.sleep(2000);
		} catch (InterruptedException e) {
		    System.out.println(e);
		}
        client.start();
		client.addNode(500, "FIFO");
        assertTrue(client.getHashRingSize() == 2);
	}

    /**
    Test if the ECS can remove a node.
    */
	@Test
	public void testRemoveNode() throws Exception{
        client.initKVService(2, 100, "FIFO", "./testECSClient3.config");
        assertTrue(client.getHashRingSize() == 2);
        try {
		    Thread.sleep(2000);
		} catch (InterruptedException e) {
		    System.out.println(e);
		}
        client.start();
		client.removeNode(0);
        assertTrue(client.getHashRingSize() == 1);
	}
}
