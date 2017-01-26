package testing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cache.*;

import java.lang.ClassLoader;

import junit.framework.TestCase;

public class LfuTest extends TestCase {
	private CacheManager cacheManager;
	
	@Before
    public void setUp() {
        cacheManager = new CacheManager(2, 10);
		cacheManager.update("A","A");
		cacheManager.update("B","B");
		cacheManager.update("C","C");
		cacheManager.update("D","D");
		cacheManager.update("E","E");
   }

    @After
    public void tearDown() {
		//do nothing
    }

	@Test
    public void testGetKeyExists() {
		System.out.println("Start of new Case:");
		String s = cacheManager.get("A");
		assertNotNull(s);
		assertTrue(s.equals("A"));
    }
	
	@Test
	public void testGetNonExistingKey(){
		System.out.println("Start of new Case:");
		String s = cacheManager.get("DoesNotExist");
		assertNull(s);
	}
	
	@Test
	public void testdeleteKeyAndCheck(){
		System.out.println("Start of new Case:");
		cacheManager.update("DeleteThisKey", "DeleteThisKey");
		String s = cacheManager.get("DeleteThisKey");
		assertNotNull(s);
		assertTrue(s.equals("DeleteThisKey"));
		assertTrue(cacheManager.delete("DeleteThisKey"));
		String t = cacheManager.get("DeleteThisKey");
		assertNull(t);
	}

	@Test
	public void testDeleteKeyThatDoesntExist(){
		System.out.println("Start of new Case: key doesnt exist");
		assertFalse(cacheManager.delete("ThisKeyDoesNotExistInCache"));	
	}

	@Test
	public void testLfuReplacementPolicy(){
		System.out.println("Start of new Case: LFU replacement Policy");
		assertTrue(cacheManager.update("F","F"));
		assertTrue(cacheManager.update("F","F"));
		assertTrue(cacheManager.update("G","G"));
		assertTrue(cacheManager.update("G","G"));
		assertTrue(cacheManager.update("H","H"));
		assertTrue(cacheManager.update("H","H"));
		assertTrue(cacheManager.update("I","I"));
		assertTrue(cacheManager.update("I","I"));
		assertTrue(cacheManager.update("J","J"));
		assertTrue(cacheManager.update("J","J"));
		
		String s = null;
		for( int i =0 ; i < 3 ; i++){
			s = cacheManager.get("A");
			assertNotNull(s);
			assertTrue(s.equals("A"));
			s = null;
		}
		for(int i = 0 ; i < 4 ; i++){
			s = cacheManager.get("B");
			assertNotNull(s);
			assertTrue(s.equals("B"));
			s = null;
		}
		cacheManager.printQueue();
		assertTrue(cacheManager.update("eleventhElement", "11"));
		cacheManager.printQueue();
		assertTrue(cacheManager.update("1","1"));
		assertTrue(cacheManager.update("1","1"));
		assertTrue(cacheManager.update("1","1"));
		assertTrue(cacheManager.update("1","1"));
		assertTrue(cacheManager.update("2","2"));
		assertTrue(cacheManager.update("2","2"));
		assertTrue(cacheManager.update("2","2"));
		assertTrue(cacheManager.update("3","3"));
		assertTrue(cacheManager.update("3","3"));
		assertTrue(cacheManager.update("3","3"));
		assertTrue(cacheManager.update("4","4"));
		assertTrue(cacheManager.update("4","4"));
		assertTrue(cacheManager.update("4","4"));
		assertTrue(cacheManager.update("4","4"));
		cacheManager.printQueue();
		String t = cacheManager.get("E");
		assertNull(t);
		t = cacheManager.get("C");
		assertNull(t);
	}

}

