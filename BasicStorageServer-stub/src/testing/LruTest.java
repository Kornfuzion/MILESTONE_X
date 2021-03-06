package testing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cache.*;

import java.lang.ClassLoader;

import junit.framework.TestCase;

public class LruTest extends TestCase {
    private CacheManager cacheManager;
	
	@Before
    public void setUp() {
        cacheManager = new CacheManager(CachePolicy.LRU, 10);
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
		assertTrue(cacheManager.delete("ThisKeyDoesNotExistInCache"));	
	}

	@Test
	public void testProperLruReplacement(){
		System.out.println("Start of new Case: lru replacement");
		String s = cacheManager.get("A");
		assertNotNull(s);
		assertTrue(s.equals("A"));
		assertTrue(cacheManager.update("F","F"));
		assertTrue(cacheManager.update("g","g"));
		assertTrue(cacheManager.update("h","h"));
		assertTrue(cacheManager.update("i","i"));
		assertTrue(cacheManager.update("j","j"));
		cacheManager.printQueue();
		assertTrue(cacheManager.update("eleventhElement", "11"));
		cacheManager.printQueue();
	}
}
