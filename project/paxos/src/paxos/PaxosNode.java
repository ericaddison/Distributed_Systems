package paxos;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import paxos.messages.Message;
import paxos.messages.MessageType;

public class PaxosNode{

	private NetworkNode netnode;
	private int id;
	private int Nprocs;
	private Logger log;
	private int lastProposalNumber = -1;
	private int proposeResponseCount = 0;
	
	private int proposeResponseN;
	private String proposeResponseV;
	
	private boolean proposer = true;
	private boolean acceptor = true;
	private boolean learner = true;
	private boolean distinguishedProposer = false;
	private boolean distinguishedLearner = false;
	
	public PaxosNode(NetworkNode node, Logger log) {
		this.log = log;
		netnode = node;
		Nprocs = netnode.getNodesInfo().size();
		this.id = netnode.getId();
		log.info("Created new " + this.getClass().getSimpleName() + " with:");
		log.info("\tid = " + id);
		
		if(id==0){
			distinguishedLearner = true;
			distinguishedProposer = true;
		}
			
	}

	public void run(){
		log.info("Starting run phase");
		if(!netnode.isRunning()){
			netnode.run();
		}
			
		startListenerLoops();
	}

	public int getId() {
		return id;
	}
	
	public int getNprocs() {
		return Nprocs;
	}
	
	public boolean isProposer() {
		return proposer;
	}

	public void setProposer(boolean proposer) {
		this.proposer = proposer;
	}

	public boolean isAcceptor() {
		return acceptor;
	}

	public void setAcceptor(boolean acceptor) {
		this.acceptor = acceptor;
	}

	public boolean isLearner() {
		return learner;
	}

	public void setLearner(boolean learner) {
		this.learner = learner;
	}

	public boolean isDistinguishedProposer() {
		return distinguishedProposer;
	}

	public void setDistinguishedProposer(boolean distinguishedProposer) {
		this.distinguishedProposer = distinguishedProposer;
	}

	public boolean isDistinguishedLearner() {
		return distinguishedLearner;
	}

	public void setDistinguishedLearner(boolean distinguishedLearner) {
		this.distinguishedLearner = distinguishedLearner;
	}

	
	
//*************************************************8
//	Proposer methods	
	
	
	public void sendProposeRequest(){
		
		// reset propose response count
		proposeResponseCount = 0;
		
		// get new proposal number
		lastProposalNumber = (lastProposalNumber == -1) ? id : lastProposalNumber+Nprocs;
		
		// get acceptor set
		List<Integer> acceptorSet = getAcceptorSet();
		
		// send proposal request to all acceptors in set
		for(int acceptorId : acceptorSet){
			Message msg = new Message(MessageType.PROPOSE_REQUEST, "v", lastProposalNumber, id);
			netnode.sendMessage(acceptorId, msg);
		}
		
		
		// TODO: Need to store last prop num???
		
	}
	
	// start by assuming everyone is an acceptor, generate set
	private List<Integer> getAcceptorSet() {
		List<Integer> list = new ArrayList<Integer>();
		for(int i=id+1; i<id+(Nprocs/2)+1; i++){
			if(i!=id)
				list.add(i%Nprocs);
		}
		return list;
	}

	public void sendAcceptRequest(){
		
	}
	
	public void receiveProposeResponse(Message msg){
		log.fine("Received PROPOSE_RESPONSE from " + msg.getId());
		
		// contents of PROPOSE_RESPONSE (the promise)
		// msg.number == YOUR request number
		// msg.value == JSON-ified Proposal
		
		// if outdated response, ignore
		if(msg.getNumber() != lastProposalNumber){
			return;
		}

		Proposal responseProposal = Proposal.fromString(msg.getValue());
		
		proposeResponseCount++;
		
		if(responseProposal.number > proposeResponseN){
			proposeResponseN = responseProposal.number;
			proposeResponseV = responseProposal.value;
		}
	}	
	
	// response when you are told "computer says no", proposal number is too low
	public void receiveNack(){
		// TODO: How to process nack?
	}
	
	
	
	
//*************************************************8
//	Acceptor methods	
	
	private Proposal acceptedProposal = null;
	private int promiseNumber = -1;
	
	public void receiveProposeRequest(Message msg){
		
		int n = msg.getNumber();
		
		// if promising
		if(n>promiseNumber){
			promiseNumber = n;
			String propString = (acceptedProposal == null) ? "" : acceptedProposal.toString();
			Message promiseMsg = new Message(MessageType.PROPOSE_RESPONSE, propString, n, id);
			sendProposeResponse(promiseMsg, msg.getId());

		} else {
			Message nackMsg = new Message(MessageType.NACK, "", n, id);
			sendNack(nackMsg, msg.getId());
		}
		
	}
	
	public void receiveAcceptRequest(){
		
	}
	
	public void sendProposeResponse(Message msg, int otherId){
		netnode.sendMessage(otherId, msg);
	}
	
	public void sendNack(Message msg, int otherId){
		netnode.sendMessage(otherId, msg);
	}
	
	public void sendAcceptResponse(){
		
	}
	
	
	// send to distinguished learner
	public void sendAcceptNotification(){
		
	}
	
	
	
	
	
//*************************************************8
//	Listen loop methods	
	

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
		
		switch(msg.getType()){
		case ACCEPT_REQUEST:
			break;
		case ACCEPT_RESPONSE:
			break;
		case INIT:
			break;
		case NACK:
			break;
		case PROPOSE_REQUEST:
			receiveProposeRequest(msg);
			break;
		case PROPOSE_RESPONSE:
			receiveProposeResponse(msg);
			break;
		default:
			break;
		}
		
		
	}
	
}
