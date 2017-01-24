package testing;

import org.junit.Test;

import app_kvServer.Storage;
import junit.framework.TestCase;

public class StorageTest extends TestCase {

    private Storage storage;

    public void setUp() {
        storage = new Storage();
    }

    public void tearDown() {

    }

    @Test
    public void testRandom() {
        assertTrue(1 == 1);
    }

}
