package paxos;

import java.io.File;

import paxos.roles.Acceptor;
import paxos.roles.Learner;
import paxos.roles.Proposer;

public class PaxosNode extends NetworkNode{

	public PaxosNode(int id, String nodeListFileName, boolean restart) {
		super(id, nodeListFileName, restart);
		getLogger().info("Created new " + this.getClass().getSimpleName() + " with:");
		getLogger().info("\tid = " + id);
		getLogger().info("\tnodeListFileName = " + nodeListFileName);
		getLogger().info("\tresart = " + restart);
		getLogger().info("Node List:");
		for(String node : getNodesInfo()){
			getLogger().info("\t" + node);
		}
	}

	private int id;
	private int Nprocs;
	
	public void run(){
		getLogger().info("Starting run phase");
		super.run();
	}
	
	// determine paxos roles by setting these values to non-null
	private Proposer proposer = null;
	private Acceptor acceptor = null;
	private Learner learner = null;
	
	
	public void setProposer(boolean isProposer){
		if(isProposer)
			proposer = new Proposer(this);
		else
			proposer = null;
	}
	
	
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
		if(args.length==2 && args[1].equals("restart"))
			restart = true;

		File file = new File(fileName);
		if (!file.exists() || file.isDirectory()){
			System.err.println("IO error for nodeList file: " + fileName);
			System.exit(2);
		}


		NetworkNode node = new PaxosNode(id, fileName, restart);
		node.run();
	}
	
}
