package testing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import datastore.*;
import cache.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ClassLoader;

import junit.framework.TestCase;

public class StorageManagerTest extends TestCase {

    private StorageManager storageManager;  
    private String storagePath;

    @Before
    public void setUp() throws FileNotFoundException {
        File testStorageFilesDirectory = new File("src/testing/resources/storagemanagertest");
        storagePath = testStorageFilesDirectory.getAbsolutePath();
        storageManager = new StorageManager(CachePolicy.LRU, 10, storagePath);
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
