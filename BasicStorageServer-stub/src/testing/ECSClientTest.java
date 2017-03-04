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
			if(in == 3000 || in == 4000) {
				assertTrue(true);
			} else {
				assertTrue(false);			
			}
		}
	}
	

	


}
