package paxos;

public class Proposal {

	private int number;
	private ProposalValue value;
	
	public Proposal(int number, ProposalValue value) {
		this.number = number;
		this.value = value;
	}
	
	public int getNumber() {
		return number;
	}
	
	public ProposalValue getValue() {
		return value;
	}
	
}
