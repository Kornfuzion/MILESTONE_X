package testing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import datastore.*;
import cache.*;

import common.messages.status.StatusType;

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
        assertEquals(s, "One");
  
        s = storageManager.get("Two");
        assertEquals(s, "Two");
   }

    @Test
    public void testSet(){
        StatusType status = storageManager.set("Two", "UpdatedTwo");
        assertTrue(status == StatusType.PUT_UPDATE);
        String s = storageManager.get("Two");
        assertNotNull(s);
        assertEquals(s, "UpdatedTwo");
    }

    @Test
    public void testDelete(){
        assertTrue(storageManager.delete("One") == StatusType.DELETE_SUCCESS);
        String s = storageManager.get("One");
        assertNull(s);
    } 
}
