package testing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.ClassLoader;

import junit.framework.TestCase;
import app_kvEcs.*;

public class ECSClientTest extends TestCase {
	private ECSClient client;
	@Before
    public void setUp() {
		//put here
		client = new ECSClient();
		
	}
	
	@After
    public void tearDown() {
		//empty
    }
	
	@Test
	public void testParseConfig(){
		Exception ex = null;
		try { 
			client.runConfig("testECSClient.config");
		} catch (Exception e) {
			ex = e;
			ex.printStackTrace();			
		}
		
		assertNull(ex);
		for(ECSNode n : client.getAvailableMachines()){
			int in = Integer.parseInt(n.getPort());
			if(in == 3000 || in == 4000 || in == 5000 || in == 6000 || in == 7000 || in == 8000 || in == 9000) {
				assertTrue(true);
			} else {
				assertTrue(false);			
			}
		}
	}

	@Test
	public void testInitService(){
		client.initKVService(3, 1000, "lfu");
		int hRSize = client.getHashRingSize();
		System.out.println("hashing size: " + hRSize);
		assertTrue(hRSize == 3);
	}

	@Test
	public void testStart(){
		client.initService(3, 1000, "lfu");
		assertTrue(client.start());
		//
	}

	@Test
	public void testStop(){
		client.initService(3,1000, "lfu");
		assertTrue(client.stop());
	}
	
	@Test
	public void testShutDown(){
		client.initService(3,1000, "lfu");
		client.start();
		assertTrue(client.shutDown());
	}
	
	@Test
	public void testStart() {
		assert(client.start());
	}

	@Test
	public void testStop() {
		assert(client.stop());
	}

	@Test
	public void testAddNode() {
		assert(client.addNode(500, "FIFO"));
	}

	@Test
	public void testRemoveNode() {
		assert(client.removeNode(0));
	}

	@Test
	public void testShutDown() {
		assert(client.shutDown());
	}
}
