package paxos.roles;

public class Learner {

	private boolean isDistinguished;
	private int Nprocs;
	
	// only if distinguished learner
	public void broadcastAcceptNotification(){
		if( !isDistinguished)
			return;
	}
	
	public void receiveAcceptNotification(){
		
	}
	
}
