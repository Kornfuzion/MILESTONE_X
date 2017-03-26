package common;

import app_kvEcs.*;
import common.*;
import common.messages.*;

import java.security.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class MetadataUtils {
    public static final int REPLICA_COUNT = 2;

    public static ECSNode getReadSuccessor(String hash, TreeSet<ECSNode> hashRing) {
        ArrayList<ECSNode> triplet = new ArrayList<ECSNode>();
        ECSNode coordinator = getSuccessor(hash, hashRing, true);
        triplet.add(coordinator);

        // Grab the next two higher nodes
        for (int i = 0; i < 2; i++) {
            ECSNode replica = hashRing.higher(coordinator);
            if (replica == null) {
                replica = hashRing.first();
            }
            triplet.add(replica);
            coordinator = replica;
        }

        // Randomly return one of the three replicas found
        int randomIndex = (new Random()).nextInt(3);
        System.out.println("FOUND RANDOM INDEX" + randomIndex);
        return triplet.get(randomIndex);
    }

    public static ECSNode getSuccessor(String hash, TreeSet<ECSNode> hashRing) {
        return getSuccessor(hash, hashRing, true);
    }

    public static ECSNode getSuccessor(String hash, TreeSet<ECSNode> hashRing, boolean lessThanEqualSuccessor) {
        if (hashRing == null) return null;
        ECSNode tempElement = new ECSNode(null, null);
        tempElement.setHashedValue(hash);                                
        ECSNode successor = lessThanEqualSuccessor ? hashRing.ceiling(tempElement) : hashRing.higher(tempElement);
        ECSNode first = hashRing.first();
        return successor == null ? first : successor;
    }

    public static String hash(String message) throws Exception {
	    byte[] bytesOfMessage = message.getBytes("UTF-8");
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] digest = md.digest(bytesOfMessage);
		final StringBuilder builder = new StringBuilder();
		for(byte b : digest) {
			builder.append(String.format("%02x", b));
		}
    	return builder.toString();
    }

    public static TreeSet<ECSNode> copyMetadata(TreeSet<ECSNode> metadata) {
        TreeSet<ECSNode> copy = new TreeSet<ECSNode>(new hashRingComparator());
        if (metadata == null) {
            return copy;
        }
        for (ECSNode node : metadata) {
            copy.add(new ECSNode(node.getPort(), node.getIP()));
        }
        return copy;
    }


    public static boolean isSuccessor(int port, ECSNode successor, TreeSet<ECSNode> metadata, CommandType command) {
        int serverIdentifier = getServerIdentifier(port, successor, metadata, command);
        return (command == CommandType.GET && serverIdentifier > 0) ||
               (command != CommandType.GET && serverIdentifier == 1);
    }

    // Return:
    // 0 - not successor
    // 1 - coordinator
    // 2 - replica 1
    // 3 - replica 2
    public static int getServerIdentifier(int port, ECSNode successor, TreeSet<ECSNode> metadata, CommandType command) {
        if (successor == null) return 1;
        int successorIndex = -1;
        for (int i = 0; i <= REPLICA_COUNT; i++) {
            if (port == Integer.parseInt(successor.getPort())) {
                successorIndex = i;
                break;
            }
            successor = metadata.higher(successor);
            if (successor == null) {
                successor = metadata.first();
            }
        }
        return successorIndex + 1;
    }
}
