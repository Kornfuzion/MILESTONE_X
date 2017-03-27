package testing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.ClassLoader;
import java.net.Socket;

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
	client.stopBeat();
        // Wait for OS to free the socket.
        Thread.sleep(10000);
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
        KVMessage message = app.getServerStatus();
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

 
    /**
    Tests the ECS client method which causes each server to reset their 2 replica sockets
    */
    @Test
    public void testUpdateReplicaSockets() {
        client.initKVService(3, 100, "FIFO", "./testECSClient2.config");
        Exception ex = null;
        try {
		    Thread.sleep(2000);
		} catch (InterruptedException e) {
		    System.out.println(e);
            ex = e;
		}
        assert(ex == null);
    }

    /**
    Tests that servers reply accordingly if we send messages as the coordinator to propagate a write
    */
    @Test
    public void testCoordinatorReplicaMessage() {
        client.initKVService(3, 100, "FIFO", "./testECSClient2.config");
        Exception ex = null;
        try {
		    Thread.sleep(2000);
		} catch (InterruptedException e) {
		    System.out.println(e);
            ex = e;
		}
        assert(ex == null);

        KVMessage response = null;
        try {
            ECSNode node = client.getHashRing().first();
            Socket serverSocket = client.getServerSocket(node.getHashedValue());
            KVMessage message = new KVMessage(CommandType.PUT)
                                        .setKey("blah")
                                        .setValue("blah")
                                        .setClientType(ClientType.COORDINATOR);
            KVMessageUtils.sendMessage(message, serverSocket.getOutputStream());
            response = KVMessageUtils.receiveMessage(serverSocket.getInputStream());
        } catch (Exception e) {
            ex = e;
            //e.printStackTrace();
        }
        assert(ex == null);
        assert(response != null && response.getStatus() != StatusType.PUT_ERROR);
    }

    /**
    Tests if we can write to only the replica that is the successor of the given hash
    */
    @Test
    public void testWriteReplica() {
        client.initKVService(3, 100, "FIFO", "./testECSClient2.config");
        Exception ex = null;
        try {
		    Thread.sleep(2000);
		} catch (InterruptedException e) {
		    System.out.println(e);
            ex = e;
		}
        assert(ex == null);

        KVMessage response = null;
        try {
            ECSNode node = client.getHashRing().first();
            Socket serverSocket = client.getServerSocket(node.getHashedValue());
            KVMessage message = new KVMessage(CommandType.PUT)
                                        .setKey("blah")
                                        .setValue("blah")
                                        .setClientType(ClientType.CLIENT);
            KVMessageUtils.sendMessage(message, serverSocket.getOutputStream());
            response = KVMessageUtils.receiveMessage(serverSocket.getInputStream());
        } catch (Exception e) {
            ex = e;
            //e.printStackTrace();
        }
        assert(ex == null);
        assert(response != null && response.getStatus() != StatusType.PUT_ERROR);
    }

        /**
    Tests if we can read from any of the 3 replicas, since we have 3 servers
    */
    @Test
    public void testReadReplica() {
        client.initKVService(3, 100, "FIFO", "./testECSClient2.config");
        Exception ex = null;
        try {
		    Thread.sleep(2000);
		} catch (InterruptedException e) {
		    System.out.println(e);
            ex = e;
		}
        assert(ex == null);

        KVMessage response = null;
        try {
            ECSNode node = client.getHashRing().first();
            Socket serverSocket = client.getServerSocket(node.getHashedValue());
            KVMessage message = new KVMessage(CommandType.GET)
                                        .setKey("blah")
                                        .setClientType(ClientType.CLIENT);
            KVMessageUtils.sendMessage(message, serverSocket.getOutputStream());
            response = KVMessageUtils.receiveMessage(serverSocket.getInputStream());
        } catch (Exception e) {
            ex = e;
            //e.printStackTrace();
        }
        assert(ex == null);
        assert(response != null && response.getStatus() != StatusType.GET_ERROR);
    }

 /** 
    Tests if heartbeat thread runs successfully given the time interval
    */	
    @Test
	public void testHeartbeat() throws Exception{
        client.initKVService(1, 100, "FIFO", "./testECSClient2.config");
       // Start the storage servers
        try {
		    Thread.sleep(2000);
		} catch (InterruptedException e) {
		    System.out.println(e);
		}
        client.start();
        ECSClient.Heartbeater heartbeater = client.getHeartbeater(client, client.getHashRing().first(), client.getHeartbeatManager());
        new Thread(heartbeater).start();
        try {
		    Thread.sleep(6000);
		} catch (InterruptedException e) {
		    System.out.println(e);
		}
        assert(heartbeater.getBeatCount() >= 2);
	}
}
