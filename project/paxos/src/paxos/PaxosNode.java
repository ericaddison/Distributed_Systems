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
	private List<Integer> distinguishedLearners;
	private boolean distinguishedProposer = false;
	private PaxosState state;
	
	
	public PaxosNode(NetworkNode node, Logger log, boolean restart) {
		this.log = log;
		netnode = node;
		Nprocs = netnode.getTotalNodeCount();
		
		// Set weight here
		// TODO: generalize for any input weight, specified in node list file
		acceptorWeights = new float[Nprocs];
		for(int i=0; i<Nprocs; i++)
			acceptorWeights[i] = 1.0f/Nprocs;
		
		this.id = netnode.getId();
		
		// parse nodeListFileTokens
		distinguishedLearners = new ArrayList<>();
		for(int i=0; i<Nprocs; i++){
			String[] toks = node.getNodeFileTokens(i);
			if (toks[NetworkNode.DL_COL].equals("1"))
				distinguishedLearners.add(i);
			acceptorWeights[i] = Float.parseFloat(toks[NetworkNode.WEIGHT_COL]);
		}
		
		if(node.getNodeFileTokens(id)[NetworkNode.DP_COL].equals("1")){
			distinguishedProposer = true;
		}
		
		if(isDistinguishedLearner())
			learnerInit();
		
		log.info("Created new " + this.getClass().getSimpleName() + " with:");
		log.info("\tid = " + id);
		log.info("\tweight = " + acceptorWeights[id]);
		log.info("\tDistinguished Proposer = " + distinguishedProposer);
		log.info("\tDistinguished Learner = " + distinguishedLearners.contains(id));
		
		// create and write out state
		state = new PaxosState();
		state.id = id;
		state.Nprocs = Nprocs;
		state.acceptorWeights = acceptorWeights;
		state.distinguishedLearners = distinguishedLearners;
		state.distinguishedProposer = distinguishedProposer;
		state.writeToFile();
	}

	public void run(){
		log.info("Starting run phase");
		if(!netnode.isRunning()){
			netnode.run();
		}
	}

	
	public void processMessage(Message msg) {
		log.finest("Processing message " + msg);
		
		switch(msg.getType()){
		case ACCEPT_REQUEST:
			receiveAcceptRequest(msg);
			break;
		case NACK:
			receiveNack(msg);
			break;
		case PREPARE_REQUEST:
			receivePrepareRequest(msg);
			break;
		case PREPARE_RESPONSE:
			receivePrepareResponse(msg);
			break;
		case ACCEPT_NOTIFICATION:
			receiveAcceptNotification(msg);
			break;
		case CHOSEN_VALUE:
			receiveChosenValueMessage(msg);
			break;
		default:
			break;
		}
	}
	
	
//*************************************************8
//	Proposer methods	
	
	private int lastProposalNumber = -1;
	private float prepareResponseSum = 0;
	private float nackSum = 0;
	private Proposal receivedProposal;
	
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
		state.lastProposalNumber = lastProposalNumber;
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
		
		// check if received proposal is null
		Proposal prop = new Proposal(lastProposalNumber, myValue);
		if(receivedProposal != null){
			prop.value = receivedProposal.value;
		}
		
		// send proposal with value to acceptors
		List<Integer> acceptorSet = getAcceptorSet();
		
		// send proposal request to all acceptors in set
		for(int acceptorId : acceptorSet){
			Message msg = new Message(MessageType.ACCEPT_REQUEST, prop.toString(), lastProposalNumber, id);
			netnode.sendMessage(acceptorId, msg);
		}
		
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
			if( receivedProposal==null || receivedProposal.number < responseProposal.number ){
				log.fine("Received promise with more recent proposal, updating values...");
				receivedProposal = responseProposal;
			} else {
				log.fine("Received promise with outdated proposal, not updating values...");
			}
		}
		
		// update response sum. If a majority (>0.5) is obtained, send accept request
		prepareResponseSum += acceptorWeights[msg.getId()];
		if(prepareResponseSum > 0.5){
			log.fine("Prepare response sum = " + prepareResponseSum + ", sending accept request");
			sendAcceptRequest();
		} else {
			log.fine("Prepare response sum = " + prepareResponseSum);
		}
		
	}	
	
	// response when you are told "computer says no", proposal number is too low
	// NACKs contain the newer proposal information that must be recorded
	public void receiveNack(Message msg){
		log.fine("Received NACK");
		Proposal responseProposal = Proposal.fromString(msg.getValue());
		if(responseProposal != null){
			if( receivedProposal==null || receivedProposal.number < responseProposal.number ){
				log.fine("Received NACK with more recent proposal, updating values...");
				receivedProposal = responseProposal;
			} else {
				log.fine("Received NACK with outdated proposal, not updating values...");
			}
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
	
	public void receiveAcceptRequest(Message msg){
		
		// parse message
		Proposal prop = Proposal.fromString(msg.getValue());
		
		// if propNumber == promiseNumber, then accept
		if(prop.number >= promiseNumber){
			log.info("Accepted new proposal: " + prop);
			acceptedProposal = prop;
			sendAcceptNotification();
		} else {
			log.info("Ignoring new proposal: " + prop);
			// otherwise send NACK
			String propString = (acceptedProposal == null) ? "" : acceptedProposal.toString();
			Message nackMsg = new Message(MessageType.NACK, propString, promiseNumber, id);
			sendNack(nackMsg, msg.getId());
		}
		

		
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
		Message msg = new Message(MessageType.ACCEPT_NOTIFICATION, acceptedProposal.toString(), acceptedProposal.number, id);
		for(Integer learnerID : distinguishedLearners){
			netnode.sendMessage(learnerID, msg);
		}
	}
	
	
	
	
//*************************************************8
//	Learner methods	
	
	Proposal[] acceptedProposals;
	private String myValue;
	private String chosenValue;
	
	
	public void learnerInit(){
		acceptedProposals = new Proposal[Nprocs];
	}
	
	
	// receive from acceptor
	public void receiveAcceptNotification(Message msg){
		
		// parse message for accepted proposal
		int accId = msg.getId();
		Proposal prop = Proposal.fromString(msg.getValue());
		
		// store proposal in acceptedProposals
		acceptedProposals[accId] = prop;
		log.fine("Received new accepted proposal from node " + accId);
		
		// check if a value has been chosen
		String chosenVal = checkForChosenValue();
		
		// inform other learners if value has been chosen
		if(chosenVal != null){
			log.info("Chosen value (" + accId + ") = " + chosenVal);
			sendChosenValue();
		} else {
			log.fine("No chosen value yet (" + accId + ")");
		}
	}	
	
	
	private String checkForChosenValue(){
		
		// reset chosenChecker to check for value
		Map<String, Float> chosenChecker = new HashMap<>();
		
		for(int i=0; i<Nprocs; i++){
			Proposal p = acceptedProposals[i];
			if(p != null){
				float sum = (chosenChecker.containsKey(p.value)) ? chosenChecker.get(p.value) : 0;
				sum += acceptorWeights[i];
				if(sum>0.5){
					chosenValue = p.value;
					return chosenValue;
				}
				chosenChecker.put(p.value, sum);
			}
		}
		
		log.finer("ChosenChecker: " + chosenChecker.toString());
		
		return null;
	}
	
	
	private void sendChosenValue(){
		log.info("Sending chosen value " + chosenValue + " to all");
		Message msg = new Message(MessageType.CHOSEN_VALUE, chosenValue, 0, id);
		for(int i=0; i<Nprocs; i++){
			if(i!=id)
				netnode.sendMessage(i, msg);
		}
	}
	
	
	private void receiveChosenValueMessage(Message msg){
		if(chosenValue == null){
			log.info("Received new chosen value from " + msg.getId() + " : " + msg.getValue());
			chosenValue = msg.getValue();
		} else {
			if(chosenValue.equals(msg.getValue()))
				log.fine("Chosen value confirmed from " + msg.getId() + " : " + msg.getValue());
			else
				log.warning("PROBLEM! CONFLICTING CHOSEN VALUE FROM " + msg.getId() + " : " + msg.getValue() + ". Current chosen value = " + chosenValue);
		}
	}
	
	
	

//*************************************************8
//	Getters and Setters
	
	
	public int getId() {
		return id;
	}
	
	public int getNprocs() {
		return Nprocs;
	}
	
	public boolean isDistinguishedProposer() {
		return distinguishedProposer;
	}

	public boolean isDistinguishedLearner() {
		return distinguishedLearners.contains(id);
	}

	public void setMyValue(String value) {
		myValue = value;
	}
	
	
}
