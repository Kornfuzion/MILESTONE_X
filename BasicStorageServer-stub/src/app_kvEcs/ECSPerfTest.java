package app_kvEcs; 

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import java.lang.InterruptedException;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.net.Socket;
import java.net.UnknownHostException;
import common.messages.*;
import common.*;
import cache.*;

public class ECSPerfTest {
	public static void main(String[] args){
		System.out.println("Starting PerfTest Main");
		final int numClients = 5;
		ECSClient client = new ECSClient();
		client.initService(5, 100, "lfu");
		
	}
}
