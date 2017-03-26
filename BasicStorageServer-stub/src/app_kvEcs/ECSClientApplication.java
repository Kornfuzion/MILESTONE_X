package app_kvEcs; 

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import logger.LogSetup;
import common.messages.*;


public class ECSClientApplication {
	
	private ECSClient client;
	private BufferedReader stdin;	
	private static final String PROMPT = "ECSClient> ";
	private boolean stop = false;
	private boolean initFlag = false;

	public ECSClientApplication(){
		client = new ECSClient();	
	}

	public void run(){
		while(!stop) {
            stdin = new BufferedReader(new InputStreamReader(System.in));
            System.out.print(PROMPT);
            
            try {
                String cmdLine = stdin.readLine();
                this.handleCommand(cmdLine);
            } catch (Exception e) {
                stop = true;
                System.out.println("CLI does not respond - Application terminated ");
            }
        }
	}
	
	private void handleCommand(String cmdLine) throws Exception {
		String[] tokens = cmdLine.split("\\s+");
		if(tokens[0].equals("add")){
			if(initFlag == true){
				if(tokens.length == 3 ){
					//TODO add error checking
					//client.add(Integer.parseInt(tokens[1]), tokens[2]);
				}
				else{
					System.out.println(PROMPT + "incorrect number of arguments for command: add");			
				}
			}else{
				System.out.println(PROMPT + "service was not initialized yet");			
			}
		}
		else if(tokens[0].equals("remove")){
			if(initFlag == true){
				if(tokens.length == 2){
					if( Integer.parseInt(tokens[1]) > client.getTotalNumberOfMachines() || Integer.parseInt(tokens[1]) < 0){
						System.out.println("Remove Error: index out of bounds");
					} else {
						//client.remove(Integer.parseInt(tokens[1]));
					}
				}
				else{
					System.out.println(PROMPT + "incorrect number of arguments for command: remove");			
				}
			}else{
				System.out.println(PROMPT + "service was not initialized yet");			
			}
		}
		else if(tokens[0].equals("init")){
			if(initFlag == false){
				if(tokens.length == 4 ){
					int numNodes = Integer.parseInt(tokens[1]);
					int cacheSize = Integer.parseInt(tokens[2]);
					String policy = tokens[3];
					initFlag = true;				
					int success = client.initKVService(numNodes, cacheSize, policy, "testECSClient.config");
					// Failed to initialize the service.
					if(success != 0) {
						System.out.println(PROMPT + "Unable to initialize storage servers. Please make sure all ports are free. Shutting down ECS.");
						client.shutDown();
						System.out.println(PROMPT + "Exiting ECS.");
           					System.exit(1);
					}
				}
				else{
					System.out.println(PROMPT + "incorrect number of arguments for command: init");			
				}
			}else{
				System.out.println(PROMPT + "init already happened, cannot init again");
			}
		}
		else if(tokens[0].equals("start")){
			if(initFlag == true){
				if(tokens.length == 1 ){
					client.start();
				}
				else{
					System.out.println(PROMPT + "incorrect number of arguments for command: start");			
				}
			}else{
				System.out.println(PROMPT + "service was not initialized yet");			
			}
		}
		else if(tokens[0].equals("stop")){
			if(initFlag == true){
				if(tokens.length == 1 ){
					client.stop();
				}
				else{
					System.out.println(PROMPT + "incorrect number of arguments for command: stop");			
				}
			}else{
				System.out.println(PROMPT + "service was not initialized yet");			
			}
		}
		else if(tokens[0].equals("shutdown")){
			if(tokens.length == 1 ){
                    client.shutDown();
                    initFlag = false;
				}
				else{
					System.out.println(PROMPT + "incorrect number of arguments for command: shutdown");			
				}
		}
		else if(tokens[0].equals("removenode")){
			if(tokens.length == 2 ){
                int serverIndex = 0;
                try {
                    serverIndex = Integer.parseInt(tokens[1]);
				    client.removeNode(serverIndex);
                } catch (Exception e) {
                    System.out.println(PROMPT + "server index must be an integer");
                }
			}
		}
		else if(tokens[0].equals("addnode")){
			if(tokens.length == 3 ){
                int cacheSize = 0;
                try {
                    cacheSize = Integer.parseInt(tokens[1]);
				    client.addNode(cacheSize, tokens[2]);
                } catch (Exception e) {
                    System.out.println(PROMPT + "cache size must be an integer.");
                }
			}	
		}
        else if(tokens[0].equals("exit")){
            client.stopBeat();
	    if (initFlag) {
                client.shutDown();
            }  
            System.exit(1);
        }
        else if(tokens[0].equals("help")) {
            printHelp();
        }
		else{
			System.out.println(PROMPT + "unknown command");
			printHelp();		
		}
	}

    private void printHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(PROMPT).append("ECS CLIENT HELP (Usage):\n");
        sb.append(PROMPT);
        sb.append("::::::::::::::::::::::::::::::::");
        sb.append("::::::::::::::::::::::::::::::::\n");
        sb.append(PROMPT).append("init <number of servers> <cache size> <cache replacement policy (LRU, LFU, FIFO)>");
        sb.append("\t initializes the storage services given the provided parameters\n");
        sb.append(PROMPT).append("start");
        sb.append("\t\t\t\t\t\t\t\t\t\t starts the initialized storage servers\n");
        sb.append(PROMPT).append("stop");
        sb.append("\t\t\t\t\t\t\t\t\t\t\t stops the storage servers\n");
        sb.append(PROMPT).append("addnode <cache size> <cache replacement policy (LRU, LFU, FIFO)>");
        sb.append("\t\t\t adds a new server in the START state given the provided parameters\n");
        sb.append(PROMPT).append("removenode <index of server>");
        sb.append("\t\t\t\t\t\t\t\t removes a storage server given <index of server>\n");
        sb.append(PROMPT).append("shutdown");
        sb.append("\t\t\t\t\t\t\t\t\t\t shutsdown all of the storage servers\n"); 
        sb.append(PROMPT).append("exit ");
        sb.append("\t\t\t\t\t\t\t\t\t\t exits the ECS client and shutsdown all storage servers");
        System.out.println(sb.toString());
    }
 
	public static void main (String[] args) {
		try{
			ECSClientApplication app = new ECSClientApplication();
			app.run();
		} catch (Exception e){
			e.printStackTrace();		
		}
	}
}
