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
	
	// Standard Ctor for initial startup
	public PaxosNode(NetworkNode node, Logger log) {
		this.log = log;
		netnode = node;
		Nprocs = netnode.getTotalNodeCount();
		
		// Set weight here
		// TODO: generalize for any input weight, specified in node list file
		acceptorWeights = new float[Nprocs];
		for(int i=0; i<Nprocs; i++)
			acceptorWeights[i] = 1.0f/Nprocs;
		
		this.id = netnode.getId();
		state = new PaxosState(id);
		
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
		log.info("\tDistinguished Learner = " + isDistinguishedLearner());
		
		// create and write out state
		state.writeToFile();
	}

	
	// restart ctor, for restarting with a state file
	public PaxosNode(NetworkNode node, Logger log, String stateFilename){
		this(node, log);
		
		// repopulate state from statefile
		log.info("\tStatefile = " + stateFilename);
		state = PaxosState.readFromFile(stateFilename);
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
		case NACK_OLDROUND:
			receiveNackOldRound(msg);
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
			receiveChosenValue(msg);
			break;
		default:
			break;
		}
	}
	
	
//*************************************************8
//	Proposer methods	
	
	public void sendPrepareRequest(){
		
		// reset propose response count
		state.prepareResponseSum = 0;
		state.nackSum = 0;
		
		// get new proposal number
		state.lastProposalNumber = (state.lastProposalNumber == -1) ? id : state.lastProposalNumber+Nprocs;
		
		// update state and write to file
		log.finest("Updated lastProposalNumber: writing state to file");
		state.writeToFile();
		
		// get acceptor set
		List<Integer> acceptorSet = getAcceptorSet();
		
		// send proposal request to all acceptors in set
		// include round number here
		for(int acceptorId : acceptorSet){
			Message msg = new Message(MessageType.PREPARE_REQUEST, ""+state.currentRound, state.lastProposalNumber, id);
			netnode.sendMessage(acceptorId, msg);
		}
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
		state.nackSum = 0;
		
		//TODO: send the accept request
		
		// check if received proposal is null
		// include round number here
		Proposal prop = new Proposal(state.lastProposalNumber, state.myValue, state.currentRound);
		if(state.receivedProposal != null){
			prop.value = state.receivedProposal.value;
		}
		
		// send proposal with value to acceptors
		List<Integer> acceptorSet = getAcceptorSet();
		
		// send proposal request to all acceptors in set
		for(int acceptorId : acceptorSet){
			Message msg = new Message(MessageType.ACCEPT_REQUEST, prop.toString(), state.lastProposalNumber, id);
			netnode.sendMessage(acceptorId, msg);
		}
		
	}
	
	
	public void receivePrepareResponse(Message msg){
		log.fine("Received PREPARE_RESPONSE from " + msg.getId());
		
		// contents of PREPARE_RESPONSE (the promise)
		// msg.number == YOUR request number
		// msg.value == JSON-ified Proposal
		
		// if outdated response, ignore
		if(msg.getNumber() != state.lastProposalNumber){
			log.fine("Outdated response, ignoring");
			return;
		}

		Proposal responseProposal = Proposal.fromString(msg.getValue());

		if(responseProposal==null){
			log.fine("Received promise from " + msg.getId());
		} else {
			if( state.receivedProposal==null || state.receivedProposal.number < responseProposal.number ){
				log.fine("Received promise with more recent proposal, updating values...");
				state.receivedProposal = responseProposal;
			} else {
				log.fine("Received promise with outdated proposal, not updating values...");
			}
		}
		
		// update response sum. 
		state.prepareResponseSum += acceptorWeights[msg.getId()];
		
		// update state and write to file
		log.finest("Updated prepareResponseSum and receivedProposal: writing state to file");
		state.writeToFile();
		
		// If a majority (>0.5) is obtained, send accept request
		if(state.prepareResponseSum > 0.5){
			log.fine("Prepare response sum = " + state.prepareResponseSum + ", sending accept request");
			sendAcceptRequest();
		} else {
			log.fine("Prepare response sum = " + state.prepareResponseSum);
		}
		
	}	
	
	// response when you are told "computer says no", proposal number is too low
	// NACKs contain the newer proposal information that must be recorded
	public void receiveNack(Message msg){
		log.fine("Received NACK");
		Proposal responseProposal = Proposal.fromString(msg.getValue());
		if(responseProposal != null){
			if( state.receivedProposal==null || state.receivedProposal.number < responseProposal.number ){
				log.fine("Received NACK with more recent proposal, updating values...");
				state.receivedProposal = responseProposal;
			} else {
				log.fine("Received NACK with outdated proposal, not updating values...");
			}
		}

		// tally NACKs. If >0.5, start a new prepare request
		state.nackSum += acceptorWeights[msg.getId()];
		
		// update state and write to file
		log.finest("Updated nackSum: writing state to file");
		state.writeToFile();
		
		if(state.nackSum > 0.5){
			log.fine("NACK sum = " + state.nackSum + ", starting new prepare request");
			sendPrepareRequest();
		}
	}
	
	
	// if found that our round is out of date, 
	public void receiveNackOldRound(Message msg){
		log.fine("Received NACK_OLDROUND");
		int theirRound = Integer.parseInt(msg.getValue());
		
		if( state.currentRound < theirRound ){
			log.finest("Updating currentRound");
			state.currentRound = theirRound;
			state.writeToFile();
			sendPrepareRequest();
		} else {
			log.fine("Received old round " + theirRound);
		}
	}
	
	
	
	
//*************************************************8
//	Acceptor methods	
	
	public void receivePrepareRequest(Message msg){
		
		int n = msg.getNumber();
		
		// check round number of incoming request
		int round = Integer.parseInt(msg.getValue());
		String propString = (state.acceptedProposal == null) ? "" : state.acceptedProposal.toString();
		
		if( round < state.currentRound ){
			log.fine("Received PREPARE for round " + round + " but I am expecting at least round " + state.currentRound);
			Message nackMsg = new Message(MessageType.NACK_OLDROUND, ""+(state.currentRound), n, id);
			sendNack(nackMsg, msg.getId());
		}
		
		// if promising
		if(n>state.promiseNumber){
			state.promiseNumber = n;
			
			// update state and write to file
			log.finest("Updared promiseNumber: writing state to file");
			state.writeToFile();
			
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
		if(prop.number >= state.promiseNumber){
			log.fine("Accepted new proposal from node " + msg.getId() + ": " + prop);
			state.acceptedProposal = prop;
			
			// update state and write to file
			log.finest("Updated acceptedProposal: writing state to file");
			state.writeToFile();
			
			sendAcceptNotification();
		} else {
			log.fine("Ignoring new proposal: " + prop);
			// otherwise send NACK
			String propString = (state.acceptedProposal == null) ? "" : state.acceptedProposal.toString();
			Message nackMsg = new Message(MessageType.NACK, propString, state.promiseNumber, id);
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
		Message msg = new Message(MessageType.ACCEPT_NOTIFICATION, state.acceptedProposal.toString(), state.acceptedProposal.number, id);
		log.fine("Preparing to send out " + msg + " to DLs");
		for(Integer learnerID : distinguishedLearners){
			netnode.sendMessage(learnerID, msg);
		}
	}
	
	
	
	
//*************************************************8
//	Learner methods	
	
	public void learnerInit(){
		state.acceptedProposals = new Proposal[Nprocs];
	}
	
	
	// receive from acceptor
	public synchronized void receiveAcceptNotification(Message msg){
		
		// parse message for accepted proposal
		int accId = msg.getId();
		Proposal prop = Proposal.fromString(msg.getValue());
		
		// store proposal in acceptedProposals
		state.acceptedProposals[accId] = prop;
		log.fine("Received new accepted proposal from node " + accId);
		
		// update state and write to file
		log.finest("Updated acceptedProposals: writing state to file");
		state.writeToFile();
		
		
		// inform other learners if value has been chosen if round has incremented
		if(state.currentRound == prop.round){
			// check if a value has been chosen
			String chosenVal = checkForChosenValue();
			
			if(chosenVal != null){
				log.fine("Chosen value (" + accId + ") = " + chosenVal);
				sendChosenValue();
				// increment round number
				state.currentRound++;
				log.finest("Updated round to " + state.currentRound + " : writing state to file");
				state.writeToFile();
			}
		}
	}	
	
	
	private synchronized String checkForChosenValue(){
		
		// reset chosenChecker to check for value
		Map<String, Float> chosenChecker = new HashMap<>();
		
		for(int i=0; i<Nprocs; i++){
			Proposal p = state.acceptedProposals[i];
			if(p != null && p.round==state.currentRound){
				float sum = (chosenChecker.containsKey(p.value)) ? chosenChecker.get(p.value) : 0;
				sum += acceptorWeights[i];
				if(sum>0.5){
					
					log.info("Determined new chosen value " + p.value + " for round " + p.round);
					state.chosenValues.put(p.round, p.value);
					
					// update state and write to file
					log.finest("Updated chosenValue for round " + state.currentRound + " : writing state to file");
					state.writeToFile();
					
					return p.value;
				}
				chosenChecker.put(p.value, sum);
			}
		}
		
		return null;
	}
	
	
	private void sendChosenValue(){
		log.fine("Sending chosen value for round " + state.currentRound + " " + state.chosenValues + " to all");
		Message msg = new Message(MessageType.CHOSEN_VALUE, state.chosenValues.get(state.currentRound), state.currentRound, id);
		for(int i=0; i<Nprocs; i++){
			netnode.sendMessage(i, msg);
		}
	}
	
	
	private synchronized void receiveChosenValue(Message msg){
		int theirRound = msg.getNumber();
		if(!state.chosenValues.containsKey(theirRound)){
			log.info("Received new chosen value from node " + msg.getId() + " for round " + theirRound + " : " + msg.getValue());
			state.chosenValues.put(theirRound, msg.getValue());
			state.currentRound = theirRound;
			state.writeToFile();
		} else {
			if(state.chosenValues.get(theirRound).equals(msg.getValue()))
				log.fine("Chosen value confirmed from node " + msg.getId() + " for round " + theirRound + " : " + msg.getValue());
			else
				log.warning("PROBLEM! CONFLICTING CHOSEN VALUE FROM " + msg.getId() + " for round " + theirRound + " : " + msg.getValue() + ". Current chosen value = " + state.chosenValues.get(theirRound));
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

	public void reset(String value) {
		log.info("Resetting Paxos state with preferred value: " + value);
		state.myValue = value;
		if(isDistinguishedLearner())
			learnerInit();
		
		// update state and write to file
		log.finest("Reset state: writing state to file");
		state.writeToFile();
	}

	
	
}
