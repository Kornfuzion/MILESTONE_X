package testing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import storage.Storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ClassLoader;

import junit.framework.TestCase;

public class StorageTest extends TestCase {

    private final String testKey1 = "test1";
    private final String testValue1 = "value1"; 
    private final String testKey2 = "test2";
    private final String testValue2 = "value2";    
    private final String testKey3 = "test3";
    private final String testValue3 = "value3";
    private final String testKey4 = "test4";
    private final String testValue4 = "value4";

    private final String testNonExistingKey = "ThisKeyDoesNotExist";
    private Storage storage;
    private String storagePath;

    @Before
    public void setUp() throws FileNotFoundException{
        File testStorageFilesDirectory = new File("src/testing/resources/storagetest");
        storagePath = testStorageFilesDirectory.getAbsolutePath();
        storage = new Storage(this.storagePath);
   }

    @After
    public void tearDown() {

    }

    @Test
    public void testGetExistingKey() {
        String value = storage.get(testKey1);
        assertNotNull(value);
        assertEquals(value, testValue1);
    }

    @Test
    public void testGetNonExistingKey() throws IOException{
        String filePath = storagePath + File.separator + testNonExistingKey;
        File file = new File(filePath);
        String value = storage.get(testNonExistingKey);
        assertNull(value);
        assertFalse(file.exists());
        
    }
    
    @Test
    public void testPutNonExistingKey() throws IOException {
        String filePath = storagePath + File.separator + testKey2;
        File file = new File(filePath);
        
        // Make sure that file does not already exist.
        file.delete();
        
        boolean successfulPut = storage.put(testKey2, testValue2);
        assertTrue(successfulPut && file.exists());

        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String checkValue = br.readLine();
        assertEquals(testValue2, checkValue); // Make sure that it wrote the correct value to the file.

        // Delete the created file.
        file.delete(); 
    }

    @Test
    public void testPutExistingKey() throws IOException {
        String filePath = storagePath + File.separator + testKey3;
        File file = new File(filePath);
 
        boolean successfulPut = storage.put(testKey3, testValue3);
        assertTrue(successfulPut && file.exists());

        // Key now exists and so does the file.

        String newValue = "newvalue";
        successfulPut = storage.put(testKey3, newValue);
        assertTrue(successfulPut && file.exists());
        
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line = br.readLine();
        br.close();

        assertEquals(line, newValue);

        // Delete the created file.
        file.delete();
    }

    @Test
    public void testDeleteExistingKey() {
        String filePath = storagePath + File.separator + testKey4;
        File file = new File(filePath);
        
        boolean successfulPut = storage.put(testKey4, testValue4);
        assertTrue(successfulPut && file.exists());

        // At this point the key now exists and so does the file.

        boolean successfulDelete = storage.delete(testKey4);
        assertTrue(successfulDelete && !file.exists());
    }

    @Test
    public void testDeleteNonExistingKey() {
        String filePath = storagePath + File.separator + testNonExistingKey;
        File file = new File(filePath);
        boolean successfulDelete = storage.delete(testNonExistingKey);
        assertTrue(!successfulDelete && !file.exists());
    }
    
}
