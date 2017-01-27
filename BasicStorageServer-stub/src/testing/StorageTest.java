package testing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import storage.Storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ClassLoader;

import junit.framework.TestCase;

public class StorageTest extends TestCase {

    private Storage storage;

    @Before
    public void setUp() throws FileNotFoundException{
        // TODO(Louis): Double check to see if it is safe to hardcode the path.
        File testStorageFilesDirectory = new File("src/testing/resources");
        storage = new Storage(testStorageFilesDirectory.getAbsolutePath());
   }

    @After
    public void tearDown() {

    }

    @Test
    public void testGetKeyExists() {
        String value = storage.get("test");
        assertNotNull(value);
        assertTrue(value.equals("value1"));
    }

    @Test
    public void testSetKeyDoesNotExist() {
        String key = "test1";
        String value = "value2";

        storage.put(key, value);
    }

}
