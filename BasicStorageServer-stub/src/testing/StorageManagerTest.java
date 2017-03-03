package testing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import datastore.StorageManager;
import cache.*;

import common.messages.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ClassLoader;

import junit.framework.TestCase;

public class StorageManagerTest extends TestCase {

    private final String testKey1 = "test1";
    private final String testValue1 = "value1"; 
    private final String testKey2 = "test2";
    private final String testKey2Clean = "key7465737432";
    private final String testValue2 = "value2";    
    private final String testKey3 = "test3";
    private final String testKey3Clean = "key7465737433";
    private final String testValue3 = "value3";
    private final String testKey4 = "test4";
    private final String testKey4Clean = "key7465737434";
    private final String testValue4 = "value4";

    private final String testNonExistingKey = "This Key Does Not Exist";
    private final String testNonExistingKeyClean = "key54686973204b657920446f6573204e6f74204578697374";
    private StorageManager storageManager;  
    private String storagePath;

    @Before
    public void setUp() throws FileNotFoundException {
        File testStorageFilesDirectory = new File("src/testing/resources/storagemanagertest");
        storagePath = testStorageFilesDirectory.getAbsolutePath();
        storageManager = new StorageManager(CachePolicy.LRU, 10, storagePath);
   }

    @After
    public void tearDown() {
        //do nothing
    }

    /**
    * Tests when a client does a get on a key that exists.
    */
    @Test
    public void testGetExistingKey() {
        String value = storageManager.get(testKey1);
        assertNotNull(value);
        assertEquals(value, testValue1);
    }

    /**
    * Tests when a client does a get on a key that does not exist.
    */
    @Test
    public void testGetNonExistingKey() throws IOException{
        String filePath = storagePath + File.separator + testNonExistingKey;
        File file = new File(filePath);
        String value = storageManager.get(testNonExistingKey);
        assertNull(value);
        assertFalse(file.exists());
        
    }
    
    /**
    * Tests when a client does a put on a key that does not exist.
    */
    @Test
    public void testPutNonExistingKey() throws IOException {
        String filePath = storagePath + File.separator + testKey2Clean;
        File file = new File(filePath);
        
        // Make sure that file does not already exist.
        file.delete();
        
        StatusType status = storageManager.set(testKey2, testValue2);
        assertTrue((status == StatusType.PUT_SUCCESS) && file.exists());

        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String checkValue = br.readLine();
        assertEquals(testValue2, checkValue); // Make sure that it wrote the correct value to the file.

        // Delete the created file.
        file.delete(); 
    }

    /**
    * Tests when a client does a put on an existing key.
    */
    @Test
    public void testPutExistingKey() throws IOException {
        String filePath = storagePath + File.separator + testKey3Clean;
        File file = new File(filePath);
 
        StatusType status = storageManager.set(testKey3, testValue3);
        assertTrue((status == StatusType.PUT_SUCCESS) && file.exists());

        // Key now exists and so does the file.

        String newValue = "newvalue";
        status = storageManager.set(testKey3, newValue);
        assertTrue((status == StatusType.PUT_UPDATE) && file.exists());
        
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line = br.readLine();
        br.close();

        assertEquals(line, newValue);

        // Delete the created file.
        file.delete();
    }

    /** 
    * Tests when client does a delete on an existing key.
    */
    @Test
    public void testDeleteExistingKey() {
        String filePath = storagePath + File.separator + testKey4Clean;
        File file = new File(filePath);
        
        StatusType status = storageManager.set(testKey4, testValue4);
        assertTrue((status == StatusType.PUT_SUCCESS) && file.exists());

        // At this point the key now exists and so does the file.

        status = storageManager.delete(testKey4);
        assertTrue((status == StatusType.DELETE_SUCCESS) && !file.exists());
    }

    /**
    * Tests when client does a delete on a key that does not exist.
    */
    @Test
    public void testDeleteNonExistingKey() {
        String filePath = storagePath + File.separator + testNonExistingKeyClean;
        File file = new File(filePath);
        StatusType status = storageManager.delete(testNonExistingKey);
        assertTrue((status == StatusType.DELETE_ERROR) && !file.exists());
    }
}
