package paxos.roles;
import java.util.ArrayList;
import java.util.List;

import paxos.PaxosNode;
import paxos.Proposal;

public class Proposer {

	private int id;
	private Proposal prop;
	private int lastProposalNumber = -1;	// should be serialized whenever updated
	private int nprocs;
	
	
	public Proposer(PaxosNode paxosNode) {
		this.id = paxosNode.getId();
		this.nprocs = paxosNode.getNprocs();
	}

	public void sendProposeRequest(){
		
		// get new proposal number
		int number = (lastProposalNumber == -1) ? id : lastProposalNumber+nprocs;
		
		// get acceptor set
		List<Integer> acceptorSet = getAcceptorSet();
		
		// send proposal request to all acceptors in set
		
		
	}
	
	// start by assuming everyone is an acceptor, generate set
	private List<Integer> getAcceptorSet() {
		List<Integer> list = new ArrayList<Integer>();
		for(int i=id+1; i<id+(nprocs/2)+1; i++){
			if(i!=id)
				list.add(i%nprocs);
		}
		return list;
	}

	public void sendAcceptRequest(){
		
	}
	
	public void receiveProposeResponse(){
		
	}	
	
	// response when you are told "computer says no", proposal number is too low
	public void receiveNack(){
		
	}
	
}
