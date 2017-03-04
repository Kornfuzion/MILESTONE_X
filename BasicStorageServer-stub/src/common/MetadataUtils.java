package common;

import app_kvEcs.*;

import java.security.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class MetadataUtils {
    public static ECSNode getSuccessor(String hash, TreeSet<ECSNode> hashRing) {
        ECSNode successor = null;
        ECSNode first = null;
        for (ECSNode node : hashRing) {
            if (first == null) {
                first = node;
            }
            if (successor == null && hash.compareTo(node.getHashedValue()) <= 0) {
                successor = node;
            }
        }
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
}
