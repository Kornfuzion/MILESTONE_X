package app_kvServer;

import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

import logger.LogSetup;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class KVServer extends Thread {

    private static Logger logger = Logger.getRootLogger();
    private int port;
    private int cacheSize;
    private String strategy;
    private ServerSocket serverSocket;
    private boolean running;

    /**
     * Start KV Server at given port
     * @param port given port for storage server to operate
     * @param cacheSize specifies how many key-value pairs the server is allowed 
     *           to keep in-memory
     * @param strategy specifies the cache replacement strategy in case the cache 
     *           is full and there is a GET- or PUT-request on a key that is 
     *           currently not contained in the cache. Options are "FIFO", "LRU", 
     *           and "LFU".
     */
    public KVServer(int port, int cacheSize, String strategy) {
        this.port = port;
        this.cacheSize = cacheSize;
        this.strategy = strategy;
    }

    /**
     * Initializes and starts the server.
     * Loops until the server should be closed.
     */
    public void run() {
        running = initializeServer();

        if (serverSocket != null) {
            while (isRunning()) {
                try {
                    Socket client = serverSocket.accept();
                    ClientConnection connection = new ClientConnection(client);
                    new Thread(connection).start();

                    logger.info("Connected to "
                                    + client.getInetAddress().getHostName()
                                    + " on port " + client.getPort());
                } catch (IOException e) {
                    logger.error("Error!    " +
                                    "Unable to establish connection. \n", e);
                }
            }
        }
        logger.info("Server stopped.");
    }

    private boolean isRunning() {
        return this.running;
    }

    /**
     * Stops the server so that it won't listen at the given port any more.
     */
    public void stopServer() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.error("Error! Unable to close coket on port: " + port, e);
        }
    }

    private boolean initializeServer() {
        logger.info("Initialize server ...");
        try {
            serverSocket = new ServerSocket(port);
            logger.info("Server listening on port: " + serverSocket.getLocalPort());
            return true;
        } catch (IOException e) {
            logger.error("Error! Cannot open server socket:");
            if (e instanceof BindException) {
                logger.error("Port " + port + " is already bound!");
            }
            return false;
        }
    }

    /**
     * Main entry point for the cho server application.
     * @param args contains the port number at args[0].
     */
    public static void main(String[] args) {
        try {
            new LogSetup("logs/server/server.log", Level.ALL);
            if (args.length != 1) {
                System.out.println("Error! Invaid number of arguements!");
                System.out.println("Usage: Server <port>!");
            } else {
                int port = Integer.parseInt(args[0]);
                // TODO(Louis): Change this to accept command line arguements for cache size
                //              and eviction policy.
                new KVServer(port, 256, "FIFO").start();
            }
        } catch (IOException e) {
            System.out.println("Error! Unable to initialize logger!");
            e.printStackTrace();
            System.exit(1);
        } catch (NumberFormatException nfe) {
            System.out.println("Error! Invalid argument <port>! Not a number!");
            System.out.println("Usage: Server <port>!");
            System.exit(1);
        }

    }
}
