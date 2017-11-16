package paxos;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import paxos.messages.Message;
import paxos.roles.Acceptor;
import paxos.roles.Learner;
import paxos.roles.Proposer;

public class PaxosNode{

	private NetworkNode netnode;
	private int id;
	private int Nprocs;
	private Logger log;
	
	public PaxosNode(NetworkNode node, Logger log) {
		this.log = log;
		netnode = node;
		Nprocs = netnode.getNodesInfo().size();
		this.id = netnode.getId();
		log.info("Created new " + this.getClass().getSimpleName() + " with:");
		log.info("\tid = " + id);
	}

	public void run(){
		log.info("Starting run phase");
		if(!netnode.isRunning()){
			netnode.run();
		}
			
		startListenerLoops();
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
	

	private void startListenerLoops(){
		// start a listener loop for all connected nodes
		for(int inode=0; inode<Nprocs; inode++){
			if(id!=inode){
				// spin off new thread
				final int ii = inode;
				Thread t = new Thread(new Runnable() {

					@Override
					public void run() {
						listenerLoop(ii);
					}
				});
				log.log(Level.FINEST, "Starting Paxos thread "+ inode);
				t.start();
			}
		}
	}
	
	
	/**
	 * Listen infinite listen loop for messages on the Paxos channel 
	 */
	private void listenerLoop(int otherID){
		Message msg = null;
		log.finer("Entering listener loop for node " + otherID);
		while(true){
			if(!netnode.isConnected(otherID)){
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {}
				continue;
			}
			
			try{
				log.finer("Listener loop connected for node " + otherID);
				while( (msg = netnode.receiveMessage(otherID)) != null){
					log.finest("Received string " + msg + " from server " + otherID);
					// process message based on type
					processMessage(msg);
				}
			} catch (Exception e){}
			finally{
				log.log(Level.WARNING, "Uh oh! Lost connection with server " + otherID + ": clearing comms");
				netnode.clearnode(otherID);
			}
		}
	}
	
	
	private void processMessage(Message msg) {
		log.info("Processing message " + msg);
	}
	
}
