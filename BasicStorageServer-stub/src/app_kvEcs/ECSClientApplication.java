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
					System.out.println("Not the correct amount of arguments for command: add");			
				}
			}else{
				System.out.println("Service was not initialized yet");			
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
					System.out.println("Not the correct amount of arguments for command: remove");			
				}
			}else{
				System.out.println("Service was not initialized yet");			
			}
		}
		else if(tokens[0].equals("init")){
			if(initFlag == false){
				if(tokens.length == 4 ){
					int numNodes = Integer.parseInt(tokens[1]);
					int cacheSize = Integer.parseInt(tokens[2]);
					String policy = tokens[3];
					initFlag = true;				
					client.initService(numNodes, cacheSize, policy);
				}
				else{
					System.out.println("Not the correct amount of arguments for command: init");			
				}
			}else{
				System.out.println("init already happened, cannot init again");
			}
		}
		else if(tokens[0].equals("start")){
			if(initFlag == true){
				if(tokens.length == 1 ){
					client.start();
				}
				else{
					System.out.println("Not the correct amount of arguments for command: start");			
				}
			}else{
				System.out.println("Service was not initialized yet");			
			}
		}
		else if(tokens[0].equals("stop")){
			if(initFlag == true){
				if(tokens.length == 1 ){
					client.stop();
				}
				else{
					System.out.println("Not the correct amount of arguments for command: stop");			
				}
			}else{
				System.out.println("Service was not initialized yet");			
			}
		}
		else if(tokens[0].equals("shutdown")){
			if(tokens.length == 1 ){
                    client.shutDown();
				}
				else{
					System.out.println("Not the correct amount of arguments for command: shutdown");			
				}
		}
		else if(tokens[0].equals("removenode")){
			if(tokens.length == 2 ){
                int serverIndex = 0;
                try {
                    serverIndex = Integer.parseInt(tokens[1]);
				    client.removeNode(serverIndex);
                } catch (Exception e) {
                    System.out.println("Server index must be an integer");
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
                    System.out.println("Cache size must be an integer.");
                }
			}	
		}
        else if(tokens[0].equals("exit")){
            client.shutDown();
            System.exit(1);
        }
		else{
			System.out.println("Unknown command");
			//printHelp();		
		}
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
