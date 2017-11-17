package paxos;

/**
 * Holds all logic for generating a new proposal number and value 
 */

public class ProposalGenerator {

	
	public static Proposal getNextProposal(int id, int nprocs, Proposal lastProposal, int round){
		
		int num = (lastProposal == null) ? id : lastProposal.getNumber() + nprocs;
		
		return new Proposal(num, new ProposalValue());
	}
	
	
}
