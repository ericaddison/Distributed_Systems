package paxos;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import paxos.messages.Message;
import paxos.messages.MessageType;

public class PaxosNode{

	private NetworkNode netnode;
	private int id;
	private int Nprocs;
	private Logger log;
	private int lastProposalNumber = -1;
	private int prepareResponseCount = 0;
	private float[] acceptorWeights;
	
	private int prepareResponseN;
	private String prepareResponseV;
	
	private boolean proposer = true;
	private boolean acceptor = true;
	private boolean learner = true;
	private boolean distinguishedProposer = false;
	private boolean distinguishedLearner = false;
	
	
	public PaxosNode(NetworkNode node, Logger log, boolean restart) {
		this.log = log;
		netnode = node;
		Nprocs = netnode.getTotalNodeCount();
		
		// Set weight here
		// TODO: generalize for any input weight, specified in node list file
		acceptorWeights = new float[Nprocs];
		for(int i=0; i<Nprocs; i++)
			acceptorWeights[i] = 1/Nprocs;
		
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
	}

	
	public void processMessage(Message msg) {
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
		case PREPARE_REQUEST:
			receivePrepareRequest(msg);
			break;
		case PREPARE_RESPONSE:
			receivePrepareResponse(msg);
			break;
		default:
			break;
		}
		
		
	}
	
	
//*************************************************8
//	Proposer methods	
	
	
	public void sendPrepareRequest(){
		
		// reset propose response count
		prepareResponseCount = 0;
		
		// get new proposal number
		lastProposalNumber = (lastProposalNumber == -1) ? id : lastProposalNumber+Nprocs;
		prepareResponseN = lastProposalNumber;
		
		// get acceptor set
		List<Integer> acceptorSet = getAcceptorSet();
		
		// send proposal request to all acceptors in set
		for(int acceptorId : acceptorSet){
			Message msg = new Message(MessageType.PREPARE_REQUEST, "v", lastProposalNumber, id);
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
	
	public void receivePrepareResponse(Message msg){
		log.fine("Received PREPARE_RESPONSE from " + msg.getId());
		
		// contents of PREPARE_RESPONSE (the promise)
		// msg.number == YOUR request number
		// msg.value == JSON-ified Proposal
		
		// if outdated response, ignore
		if(msg.getNumber() != lastProposalNumber){
			log.fine("Outdated response, ignoring");
			return;
		}

		Proposal responseProposal = Proposal.fromString(msg.toString());
		
		prepareResponseCount++;
		
		if(responseProposal.number > prepareResponseN){
			log.fine("Received newer proposal number, updating...");
			prepareResponseN = responseProposal.number;
			prepareResponseV = responseProposal.value;
		} else {
			log.fine("Received older or equal proposal number, not updating...");
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
	
	public void receivePrepareRequest(Message msg){
		
		int n = msg.getNumber();
		
		// if promising
		if(n>promiseNumber){
			promiseNumber = n;
			String propString = (acceptedProposal == null) ? "" : acceptedProposal.toString();
			Message promiseMsg = new Message(MessageType.PREPARE_RESPONSE, propString, n, id);
			sendPrepareResponse(promiseMsg, msg.getId());

		} else {
			Message nackMsg = new Message(MessageType.NACK, "", n, id);
			sendNack(nackMsg, msg.getId());
		}
		
	}
	
	public void receiveAcceptRequest(){
		
	}
	
	public void sendPrepareResponse(Message msg, int otherId){
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
//	Getters and Setters
	
	
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
	
	
}
