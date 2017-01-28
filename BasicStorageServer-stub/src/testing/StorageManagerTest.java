package testing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import datastore.*;
import cache.*;

import java.lang.ClassLoader;

import junit.framework.TestCase;

public class StorageManagerTest extends TestCase {

	private StorageManager storageManager;	

	@Before
    public void setUp() {
        storageManager = new StorageManager(CachePolicy.LRU, 10);
		storageManager.set("One", "One");
		storageManager.set("Two", "Two");
   }

    @After
    public void tearDown() {
		//do nothing
    }

	@Test
	public void testGet(){
		String s = storageManager.get("One");
		assertNotNull(s);
		assertTrue(s.equals("One"));
	}
	/*
	@Test
	public void testSet(){
		assertTrue(storageManager.set("Two", "UpdatedTwo"));
		String s = storageManager.get("Two");
		assertNotNull(s);
		assertTrue(s.equals("UpdatedTwo"));
	}

	@Test
	public void testDelete(){
		assertTrue(storageManager.delete("One"));
		String s = storageManager.get("One");
		assertNull(s);
	}
	*/
}
