package app_kvClient.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import logger.LogSetup;

import app_kvClient.client.*;
import app_kvEcs.*;
import common.messages.*;
import common.*;

public class Application implements ClientSocketListener {

    private static Logger logger = Logger.getRootLogger();
    private static final String PROMPT = "EchoClient> ";
    private BufferedReader stdin;
    private KVClient client = null;
    private boolean stop = false;

    private static final int KEY_SIZE_LIMIT = 20;
    private static final int VALUE_SIZE_LIMIT = 120 * 1024;

    private static String ROOT_ADDRESS = "localhost";
    private static int ROOT_PORT = 4000;
    
    private TreeSet<ECSNode> metadata;

    private String serverAddress;
    private int serverPort;

	private boolean synchronous;

    public Application(boolean synchronous) {
        this.metadata = null;
		this.synchronous = synchronous;
        initMetadata();
    }    

    public void run() {
        while(!stop) {
            stdin = new BufferedReader(new InputStreamReader(System.in));
            System.out.print(PROMPT);
            
            try {
                String cmdLine = stdin.readLine();
                this.handleCommand(cmdLine);
            } catch (Exception e) {
                e.printStackTrace();
                stop = true;
                printError("CLI does not respond - Application terminated ");
            }
        }
    }
    
    private boolean checkKey(String key) {
        return key.length() <= KEY_SIZE_LIMIT; 
    } 

    private boolean checkValue(String value) {
        return value.length() <= VALUE_SIZE_LIMIT;  
    } 

    // THIS FUNCTION IS CURRENTLY A MASSIVE CLUSTERFUCK, FIX LATER
    private void handleCommand(String cmdLine) throws Exception {
        String[] tokens = cmdLine.split("\\s+");

        if(tokens[0].equals("quit")) {  
            stop = true;
            disconnect();
            System.out.println(PROMPT + "Application exit!");
        
        } else if (tokens[0].equals("connect")){
            if(tokens.length == 3) {
                try{
                    serverAddress = tokens[1];
                    serverPort = Integer.parseInt(tokens[2]);
                    connect(serverAddress, serverPort);
                } catch(NumberFormatException nfe) {
                    printError("No valid address. Port must be a number!");
                    logger.info("Unable to parse argument <port>", nfe);
                } catch (UnknownHostException e) {
                    printError("Unknown Host!");
                    logger.info("Unknown Host!", e);
                } catch (IOException e) {
                    printError("Could not establish connection!");
                    logger.warn("Could not establish connection!", e);
                }
            } else {
                printError("Invalid number of parameters!");
            }
            
        } else  if (tokens[0].equals("send")) {
            if(tokens.length >= 2) {
                if(client != null && client.isRunning()){
                    StringBuilder msg = new StringBuilder();
                    for(int i = 1; i < tokens.length; i++) {
                        msg.append(tokens[i]);
                        if (i != tokens.length -1 ) {
                            msg.append(" ");
                        }
                    }   
                    sendChatMessage(msg.toString());
                } else {
                    printError("Not connected!");
                }
            } else {
                printError("No message passed!");
            }
        } else  if (tokens[0].equals("get")) {
            if(tokens.length >= 2) {
                String key = tokens[1];
                if (checkKey(key)) { 
                    connectToAppropriateServer(key, CommandType.GET);
                    client.get(key);
                }
                else {
                    printError("Key length too long!"); 
                } 
            } else {
                printError("No key passed!");
            }
            
        } else  if (tokens[0].equals("put")) {
            if(tokens.length >= 3) {
                String key = tokens[1];
                // grab the rest of the commmandline args as a solid chunk for the value
                String value = (cmdLine.substring(cmdLine.indexOf(key) + key.length())).trim();
                if (checkKey(key) && checkValue(value)) {
                   connectToAppropriateServer(key, CommandType.PUT);
                   client.put(key, value);
                } 
                else {
                   printError("Key/value length too long!");
                }
            } else if(tokens.length >= 2) {
                String key = tokens[1];
                if (checkKey(key)) {
                    connectToAppropriateServer(key, CommandType.DELETE);
                    client.delete(key);
                }
                else {
                    printError("Key length too long!");
                }
            } else {
                printError("No message passed!");
            }
        } else  if (tokens[0].equals("delete")) {
            if(tokens.length >= 2) {
                String key = tokens[1];
                connectToAppropriateServer(key, CommandType.DELETE);
                client.delete(key);
            } else {
                printError("No message passed!");
            }
        } else if(tokens[0].equals("disconnect")) {
            disconnect();
            
        } else if(tokens[0].equals("logLevel")) {
            if(tokens.length == 2) {
                String level = setLevel(tokens[1]);
                if(level.equals(LogSetup.UNKNOWN_LEVEL)) {
                    printError("No valid log level!");
                    printPossibleLogLevels();
                } else {
                    System.out.println(PROMPT + 
                            "Log level changed to level " + level);
                }
            } else {
                printError("Invalid number of parameters!");
            }
            
        } else if(tokens[0].equals("help")) {
            printHelp();
        } else {
            printError("Unknown command");
            printHelp();
        }
    }
    
	public KVMessage get(String key) throws Exception {
        connectToAppropriateServer(key, CommandType.GET);
        client.get(key);
		KVMessage reply = client.getLatestMessage();
		handleNewMessage(reply);
		return reply;
	}

	public KVMessage put(String key, String value) throws Exception {
        connectToAppropriateServer(key, CommandType.PUT);
        client.put(key, value);
		KVMessage reply = client.getLatestMessage();
		handleNewMessage(reply);
		return reply;
	}

    public KVMessage getServerStatus() throws Exception {
        connect(ROOT_ADDRESS, ROOT_PORT);
        client.initMetadata();
        KVMessage message = client.getLatestMessage();
        disconnect();
        return message;
    }

    private void sendChatMessage(String msg) {
        try {
            client.sendChatMessage(msg);
        } catch (Exception e) {
            printError("Unable to send message!");
            disconnect();
        }
    }

    private void connect(String address, int port) 
            throws UnknownHostException, IOException {
        client = new KVClient(address, port);
        client.addListener(this);
		if (!synchronous) {
        	client.start();
		}
    }
    
    private void disconnect() {
        if(client != null) {
            client.closeConnection();
            client = null;
        }
    }
    
    private ECSNode getServerForKey(String key, CommandType command) {
        try {
            switch (command) {
                case GET:
                    return MetadataUtils.getReadSuccessor(MetadataUtils.hash(key), metadata);
                default:
                    return MetadataUtils.getSuccessor(MetadataUtils.hash(key), metadata);       
            }
        } catch (Exception e) {
            //for some reason we couldn't...?
            e.printStackTrace();
            return null;
        }
    }

    private void connectToAppropriateServer(String key, CommandType command) {
        if(client == null || !client.isRunning()){
            ECSNode server = getServerForKey(key, command);
            if (server != null) {
                try {
                    System.out.println("FOUND SERVER PORT=" + server.getPort() + " IP = "+server.getIP());
                    connect(server.getIP(), Integer.parseInt(server.getPort()));
                } catch (Exception e) {
                    //aiyah... couldn't connect for some reason
                    e.printStackTrace();
                }
            }
            else {
                System.out.println("Hmm, strange. We couldn't find the appropriate server for key " + key);
            }
        }
    }

    private void printHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(PROMPT).append("ECHO CLIENT HELP (Usage):\n");
        sb.append(PROMPT);
        sb.append("::::::::::::::::::::::::::::::::");
        sb.append("::::::::::::::::::::::::::::::::\n");
        sb.append(PROMPT).append("connect <host> <port>");
        sb.append("\t establishes a connection to a server\n");
        sb.append(PROMPT).append("get <key>");
        sb.append("\t\t\t retrieves a key-value pair corresponding to the given <key>\n");
        sb.append(PROMPT).append("put <key> <value>");
        sb.append("\t\t stores a key-value pair with the given <key> <value>, or updates existing pair with key <key>\n");
     // sb.append(PROMPT).append("delete <key>");
     // sb.append("\t\t deletes a key-value pair corresponding to the given <key>\n");
     // sb.append(PROMPT).append("send <text message>");
     // sb.append("\t\t sends a text message to the server \n");
        sb.append(PROMPT).append("disconnect");
        sb.append("\t\t\t disconnects from the server \n");
        
        sb.append(PROMPT).append("logLevel");
        sb.append("\t\t\t changes the logLevel \n");
        sb.append(PROMPT).append("\t\t\t\t ");
        sb.append("ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \n");
        
        sb.append(PROMPT).append("quit ");
        sb.append("\t\t\t exits the program");
        System.out.println(sb.toString());
    }
    
    private void printPossibleLogLevels() {
        System.out.println(PROMPT 
                + "Possible log levels are:");
        System.out.println(PROMPT 
                + "ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF");
    }

    private String setLevel(String levelString) {
        
        if(levelString.equals(Level.ALL.toString())) {
            logger.setLevel(Level.ALL);
            return Level.ALL.toString();
        } else if(levelString.equals(Level.DEBUG.toString())) {
            logger.setLevel(Level.DEBUG);
            return Level.DEBUG.toString();
        } else if(levelString.equals(Level.INFO.toString())) {
            logger.setLevel(Level.INFO);
            return Level.INFO.toString();
        } else if(levelString.equals(Level.WARN.toString())) {
            logger.setLevel(Level.WARN);
            return Level.WARN.toString();
        } else if(levelString.equals(Level.ERROR.toString())) {
            logger.setLevel(Level.ERROR);
            return Level.ERROR.toString();
        } else if(levelString.equals(Level.FATAL.toString())) {
            logger.setLevel(Level.FATAL);
            return Level.FATAL.toString();
        } else if(levelString.equals(Level.OFF.toString())) {
            logger.setLevel(Level.OFF);
            return Level.OFF.toString();
        } else {
            return LogSetup.UNKNOWN_LEVEL;
        }
    }
    
    private String bracketizeArgs(String... args) {
        if (args.length == 0) {
            return null; 
        }
        String result = "<";
        boolean firstSet = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null && args[i].length() > 0) {
                if (firstSet) {
                    result = result + ",";
                }
                else {
                    firstSet = true;
                }   
                result = result + args[i]; 
            }
        }
        return result + ">";
    } 

    @Override
    public void handleNewMessage(KVMessage message) throws Exception {
        if(!stop) {
            System.out.println(message.getStatus().getStringName() + 
                               bracketizeArgs(message.getKey(), message.getValue(), message.getMessage()));
            System.out.print(PROMPT);
            if (message.getCommand() == CommandType.INIT_CLIENT_METADATA ||
               (message.getStatus() == StatusType.REROUTE)) {
                    this.metadata = message.getMetadata();
                    System.out.println("UPDATED METADATA");
                    for (ECSNode node : metadata) {
                        System.out.println(node.getPort() + " " + node.getIP());
                    }
                    disconnect();
                    
                    // Try again if we failed our request due to stale metadata
                    if (message.getStatus() == StatusType.REROUTE) {
                        String key = message.getKey();
                        connectToAppropriateServer(key, message.getCommand());

                        switch (message.getCommand()) {
                            case GET:
                                client.get(key);
                                break;
                            case PUT:
                                client.put(key, message.getValue());
                                break;
                            case DELETE:
                                client.delete(key);
                                break;
                        }
						if (synchronous) {
							KVMessage reply = client.getLatestMessage();
							//System.out.println(reply.getStatus().getStringName() + bracketizeArgs(reply.getKey(), reply.getValue(), reply.getMessage()));
							//System.out.print(PROMPT);
						}
                    }
            }
        }
    }
    
    @Override
    public void handleStatus(SocketStatus status) {
        if(status == SocketStatus.CONNECTED) {

        } else if (status == SocketStatus.DISCONNECTED) {
            System.out.print(PROMPT);
            System.out.println("Connection terminated: " 
                    + serverAddress + " / " + serverPort);
            
        } else if (status == SocketStatus.CONNECTION_LOST) {
            System.out.println("Connection lost: " 
                    + serverAddress + " / " + serverPort);
            System.out.print(PROMPT);
        }
        
    }

    private void printError(String error){
        System.out.println(PROMPT + "Error! " +  error);
    }
    
    public void initMetadata() {
        try {
            connect(ROOT_ADDRESS, ROOT_PORT);
            client.initMetadata();
			if (synchronous) {
                KVMessage message = client.getLatestMessage();
				this.metadata = message.getMetadata();
				disconnect();
			}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }   

    /**
     * Main entry point for the echo server application. 
     * @param args contains the port number at args[0].
     */
    public static void main(String[] args) {
        try {
            new LogSetup("logs/client/client.log", Level.OFF);
            Application app = new Application(false);
            app.run();
        } catch (IOException e) {
            System.out.println("Error! Unable to initialize logger!");
            e.printStackTrace();
            System.exit(1);
        }
    }

}
