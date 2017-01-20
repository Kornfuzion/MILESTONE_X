package logging;

import java.io.IOException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * Represents the initialization for the server logging with Log4J.
 */
public class LogSetup {

	private static Logger logger = Logger.getRootLogger();
	private String logdir;
	
	/**
	 * Initializes the logging for the echo server. Logs are appended to the 
	 * console output and written into a separated server log file at a given 
	 * destination.
	 * 
	 * @param logdir the destination (i.e. directory + filename) for the 
	 * 		persistent logging information.
	 * @throws IOException if the log destination could not be found.
	 */
	public LogSetup(String logdir, Level logLevel) throws IOException {
		this.logdir = logdir;
		initialize(logLevel);
	}

	private void initialize(Level logLevel) throws IOException {
		PatternLayout layout = new PatternLayout( "%d{ISO8601} %-5p [%t] %c: %m%n" );
		FileAppender fileAppender = new FileAppender( layout, logdir, true );		
	    
	    ConsoleAppender consoleAppender = new ConsoleAppender(layout);
		logger.addAppender(consoleAppender);
		
		logger.addAppender(fileAppender);
		// ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF:
		logger.setLevel(logLevel);
	}
	
}
