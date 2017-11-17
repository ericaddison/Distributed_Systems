package paxos.application;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import paxos.NetworkNode;
import paxos.PaxosNode;

public class PretendApp {

	private final Logger log = Logger.getLogger(this.getClass().getCanonicalName());
	private Level logLevel = Level.ALL;
	
	private NetworkNode netnode;
	private PaxosNode paxnode;
	private int id;
	
	
	public PretendApp(int id, String nodeListFileName, boolean restart) {
		this.id = id;
		setupLogger();
		
		log.info("Created new " + this.getClass().getSimpleName() + " with:");
		log.info("\tid = " + id);
		
		netnode = new NetworkNode(id, nodeListFileName, restart, log);
		paxnode = new PaxosNode(netnode, log);

	}
	
	public void run(){
		netnode.run();
		paxnode.run();
		/*
		if(id==0){
			try {
				Thread.sleep(5000);
				paxnode.sendProposeRequest();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		*/
	}
	
	
	private void setupLogger(){
		// set logging format
		System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$s ::: %2$s ::: %5$s %6$s%n");
		
		// NO initial logging handlers
				log.getParent().removeHandler(log.getParent().getHandlers()[0]);
		
		try {
			FileHandler fh = new FileHandler("logs/log_" + id + ".log");
			fh.setFormatter(new SimpleFormatter());
			fh.setLevel(logLevel);
			log.addHandler(fh);
			log.setLevel(logLevel);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	
//****************************************************************
//	main()
//****************************************************************
	
	/**
	 * Run the main program
	 * 
	 * @param args
	 *            command line input. Expects [id] [nodeList file] [optional "restart"]
	 *            
	 * Format of nodeList file:
	 * 127.0.0.1:8000
	 * 127.0.0.1:8005
	 * [ip]:[port]
	 * ...
	 */
	public static void main(String[] args) {
		if (args.length < 2 || args.length > 3) {
			System.out.println("ERROR: Provide 2 or 3 arguments");
			System.out.println("\t(1) <int>: process id, between 0 and number of nodes in node list file");
			System.out.println("\t(2) <file>: node list file");
			System.out.println("\t(3) <restart>: optional flag to restart failed server");
			System.exit(-1);
		}

		int id=0;
		try{
			id = Integer.parseInt(args[0]);
		} catch (NumberFormatException e){
			System.err.println("Error parsing process id from input string " + args[0]);
			System.exit(1);
		}
		
		String fileName = args[1];
		
		boolean restart = false;
		if(args.length==3 && args[2].equals("restart"))
			restart = true;

		File file = new File(fileName);
		if (!file.exists() || file.isDirectory()){
			System.err.println("IO error for nodeList file: " + fileName);
			System.exit(2);
		}


		PretendApp app = new PretendApp(id, fileName, restart);
		app.run();
	}
	
	
}
