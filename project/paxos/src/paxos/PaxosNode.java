package paxos;

import paxos.roles.Acceptor;
import paxos.roles.Learner;
import paxos.roles.Proposer;

public class PaxosNode {

	private int id;
	private int Nprocs;
	
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
	
}
