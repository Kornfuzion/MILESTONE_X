package testing;

import app_kvEcs.*;
import common.messages.*;
import common.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.ClassLoader;
import java.util.*;

import junit.framework.TestCase;

public class MetadataTest extends TestCase {
    private TreeSet<ECSNode> metadata;
    ArrayList<ECSNode> nodes;

    @Before
    public void setUp() {
        metadata = new TreeSet<ECSNode>(new hashRingComparator());
        nodes = new ArrayList<ECSNode>();
        nodes.add(new ECSNode("4000", "127.0.0.1")); // hashvalue : 022009c99acb25a7cd63a327c37bd79e
        nodes.add(new ECSNode("5000", "127.0.0.1")); // hashvalue : e6fc2d13734c525c7d17d422866675de
        nodes.add(new ECSNode("3000", "127.0.0.1")); // hashvalue : e94269b7dff0bcf487e71a7d65354a0c
        metadata.add(nodes.get(0)); 
        metadata.add(nodes.get(1)); 
        metadata.add(nodes.get(2)); 
    }
	
    @After
    public void tearDown() {
        //empty
    }
	
    @Test
    public void testGetSuccessor() {
        ECSNode successor = MetadataUtils.getSuccessor("d22009c99acb25a7cd63a327c37bd79e", metadata);
        assert (successor == nodes.get(0));
    }

    @Test
    public void testGetReadSuccessor() {
        ECSNode successor = MetadataUtils.getReadSuccessor("d22009c99acb25a7cd63a327c37bd79e", metadata);
        assert(successor == nodes.get(0) || successor == nodes.get(1) || successor == nodes.get(2));
    }

    @Test
    public void testGetServerIdentifier() {
        int serverIdentifier = MetadataUtils.getServerIdentifier(3000, nodes.get(0), metadata, CommandType.PUT);
        assert(serverIdentifier == 2);
    }

    @Test
    public void testIsSuccessor() {
        boolean isSuccessor = MetadataUtils.isSuccessor(3000, nodes.get(0), metadata, CommandType.GET);
        assert(isSuccessor == true);
    }
    @Test
    public void testNotIsSuccessor() {
        boolean isSuccessor = MetadataUtils.isSuccessor(3000, nodes.get(0), metadata, CommandType.PUT);
        assert(isSuccessor == false);
    }
    
}
