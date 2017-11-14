package paxos;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class to build on. This class will only worry about creating and managing TCP
 * connections to other nodes. Algorithmic pieces will act as decorators on this base class. 
 * @author eyms
 *
 */
public class NetworkNode {

	private int port;
	private List<NetworkNode> otherNodes;
	
	// Logging
	private final Logger log = Logger.getLogger(this.getClass().getCanonicalName());
	private Level logLevel = Level.ALL;
	
	
	
	public NetworkNode(int port) {
		this.port = port;
		this.otherNodes = new ArrayList<>();
		
		// NO initial logging handlers
		log.getParent().removeHandler(log.getParent().getHandlers()[0]);
	}
	
	public int getPort(){
		return port;
	}
	
	public Logger getLogger(){
		return log;
	}

	
	
	
	
}
