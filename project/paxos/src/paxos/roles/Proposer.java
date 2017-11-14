package paxos.roles;
import paxos.PaxosNode;
import paxos.Proposal;

public class Proposer {

	private int id;
	private Proposal prop;
	private int lastProposalNumber;	// should be serialized whenever updated 
	
	
	public Proposer(PaxosNode paxosNode) {
	}

	public void sendProposeRequest(){
		
	}
	
	public void sendAcceptRequest(){
		
	}
	
	public void receiveProposeResponse(){
		
	}	
	
	// response when you are told "computer says no", proposal number is too low
	public void receiveNack(){
		
	}
	
}
