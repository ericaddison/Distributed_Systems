package paxos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import paxos.messages.Message;
import paxos.messages.MessageType;

public class PaxosNode{

	private NetworkNode netnode;
	private int id;
	private int Nprocs;
	private Logger log;
	private float[] acceptorWeights;
	
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
		
		if(learner)
			learnerInit();
			
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
	
	private int lastProposalNumber = -1;
	private int prepareResponseSum = 0;
	private int nackSum = 0;
	private int prepareResponseN;
	private String prepareResponseV;
	
	public void sendPrepareRequest(){
		
		// reset propose response count
		prepareResponseSum = 0;
		nackSum = 0;
		
		// get new proposal number
		lastProposalNumber = (lastProposalNumber == -1) ? id : lastProposalNumber+Nprocs;
		
		// get acceptor set
		List<Integer> acceptorSet = getAcceptorSet();
		
		// send proposal request to all acceptors in set
		for(int acceptorId : acceptorSet){
			Message msg = new Message(MessageType.PREPARE_REQUEST, "", lastProposalNumber, id);
			netnode.sendMessage(acceptorId, msg);
		}
		
		
		// TODO: Need to store last prop num??? (serialize)
		
	}
	
	// start by assuming everyone is an acceptor, generate set
	private List<Integer> getAcceptorSet() {
		List<Integer> list = new ArrayList<Integer>();
		for(int i=id+1; i<id+(Nprocs/2)+2; i++){
			list.add(i%Nprocs);
		}
		return list;
	}

	
	public void sendAcceptRequest(){
		nackSum = 0;
		
		//TODO: send the accept request
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

		Proposal responseProposal = Proposal.fromString(msg.getValue());

		if(responseProposal==null){
			log.fine("Received promise from " + msg.getId());
		} else {
			if( prepareResponseN < responseProposal.number ){
				log.fine("Received promise with more recent proposal, updating values...");
				prepareResponseN = responseProposal.number;
				prepareResponseV = responseProposal.value;
			} else {
				log.fine("Received promise with outdated proposal, not updating values...");
			}
		}
		
		// update response sum. If a majority (>0.5) is obtained, send accept request
		prepareResponseSum += acceptorWeights[msg.getId()];
		if(prepareResponseSum > 0.5){
			log.fine("Prepare response sum = " + nackSum + ", sending accept request");
			sendAcceptRequest();
		}
		
	}	
	
	// response when you are told "computer says no", proposal number is too low
	// NACKs contain the newer proposal information that must be recorded
	public void receiveNack(Message msg){
		log.fine("Received NACK");
		Proposal responseProposal = Proposal.fromString(msg.getValue());
		if(responseProposal!=null){
			prepareResponseN = responseProposal.number;
			prepareResponseV = responseProposal.value;
		}

		// tally NACKs. If >0.5, start a new prepare request
		nackSum += acceptorWeights[msg.getId()];
		if(nackSum > 0.5){
			log.fine("NACK sum = " + nackSum + ", starting new prepare request");
			sendPrepareRequest();
		}
	}
	
	
	
	
//*************************************************8
//	Acceptor methods	
	
	private Proposal acceptedProposal = null;
	private int promiseNumber = -1;
	
	public void receivePrepareRequest(Message msg){
		
		int n = msg.getNumber();
		String propString = (acceptedProposal == null) ? "" : acceptedProposal.toString();
		
		// if promising
		if(n>promiseNumber){
			promiseNumber = n;
			Message promiseMsg = new Message(MessageType.PREPARE_RESPONSE, propString, n, id);
			sendPrepareResponse(promiseMsg, msg.getId());
		} else {
			Message nackMsg = new Message(MessageType.NACK, propString, n, id);
			sendNack(nackMsg, msg.getId());
		}
		
	}
	
	public void receiveAcceptRequest(){
		
		// parse message
		
		// get proposal number
		
		// if propNumber == promiseNumber, then accept
		
		// otherwise send NACK?
		
	}
	
	
	public void sendPrepareResponse(Message msg, int otherId){
		netnode.sendMessage(otherId, msg);
	}
	
	
	public void sendNack(Message msg, int otherId){
		netnode.sendMessage(otherId, msg);
	}
	
	
	// send to distinguished learner(s)
	public void sendAcceptNotification(){
		// TODO: Need to store accepted proposal (serialize)
	}
	
	
	
	
//*************************************************8
//	Learner methods	
	
	Proposal[] acceptedProposals;
	Map<String, Float> chosenChecker;
	
	public void learnerInit(){
		acceptedProposals = new Proposal[Nprocs];
		chosenChecker = new HashMap<>();
	}
	
	// receive from acceptor
	public void receiveAcceptNotification(Message msg){
		
		// parse message for accepted proposal
		
		// store proposal in acceptedProposals
		
		// check if a value has been chosen
		
		// inform other learners if value has been chosen
		
	}	
	
	private String checkForChosenValue(){
		
		// reset chosenChecker to check for value
		chosenChecker.clear();
		
		for(int i=0; i<Nprocs; i++){
			Proposal p = acceptedProposals[i];
			if(p != null){
				float sum = (chosenChecker.containsKey(p.value)) ? chosenChecker.get(p.value) : 0;
				if(sum>0.5)
					return p.value;
				chosenChecker.put(p.value, sum+acceptorWeights[i]);
			}
		}
		
		return null;
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
