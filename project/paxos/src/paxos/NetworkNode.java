package paxos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import paxos.messages.Message;
import paxos.messages.MessageType;

/**
 * Base class to build on. This class will only worry about creating and managing TCP
 * connections to other nodes. Algorithmic pieces will act as decorators on this base class. 
 * @author eyms
 *
 */
public class NetworkNode {
	
	private static final int TIMEOUT = 500;
	private static final long WAIT_TIME = 500;
	private static final long MAX_WAIT_TIME = 60000;
	private int id;
	private List<NodeInfo> nodes;
	private Thread connectThread;
	private ServerSocket serverSocket;
	private boolean restart;
	
	
	// Logging
	private final Logger log = Logger.getLogger(this.getClass().getCanonicalName());
	private Level logLevel = Level.ALL;
	
	
	public NetworkNode(int id, String nodeListFileName, boolean restart) {
		this.id = id;
		this.restart = restart;
		parseNodeFile(nodeListFileName);
		setupLogger();
	}
	
	public void run(){
		networkInit();		
	}
	
	public int getId() {
		return id;
	}
	
	private void parseNodeFile(String filename) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename)))) {

			// remaining lines = server locations
			String nextServer = "";
			nodes = new ArrayList<>();
			while ((nextServer = br.readLine()) != null) {
				String[] serverToks = nextServer.split(":");
				NodeInfo newNode = new NodeInfo();
				newNode.address = InetAddress.getByName(serverToks[0]);
				newNode.port = Integer.parseInt(serverToks[1]);
				nodes.add(newNode);
			}
			if(id<0 || id>=nodes.size())
				throw new ArrayIndexOutOfBoundsException();
			nodes.get(id).connected = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getPort(){
		return nodes.get(id).port;
	}
	
	public Logger getLogger(){
		return log;
	}
	
	public int getConnectedCount(){
		return nodes
				.stream()
				.mapToInt(NodeInfo::isConnected)
				.reduce(0, (a, b) -> a + b);
	}
	
	public List<String> getNodesInfo(){
		return nodes.stream().map(NodeInfo::toString).collect(Collectors.toList());
	}


	private void setupLogger(){
		// set logging format
		System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$s ::: %2$s ::: %5$s %6$s%n");
		
		// NO initial logging handlers
				log.getParent().removeHandler(log.getParent().getHandlers()[0]);
		
		try {
			FileHandler fh = new FileHandler("logs/log_" + id + ".log");
			fh.setFormatter(new SimpleFormatter());
			fh.setLevel(logLevel);
			log.addHandler(fh);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}

		ConsoleHandler ch = new ConsoleHandler();
		ch.setLevel(logLevel);
		log.addHandler(ch);
		log.setLevel(logLevel);
	}
	
	
	/**
	 * Send a message to another node by id
	 */
	public boolean sendMessage(int theirId, Message msg) {
		NodeInfo node = nodes.get(theirId);
		if(!node.connected){
			log.warning("Error sending message to node " + theirId + ": node not connected");
			return false;
		}
		
		log.finest("Sending message \"" + msg + "\" to node " + theirId );
		node.writer.println(msg.toString());
		node.writer.flush();
		return true;
	}
	
	
	/**
	 * Receive a message from another node   
	 */
	public Message receiveMessage(int theirId){
		String msg = null;
		NodeInfo node = nodes.get(theirId);
		try {
			msg = node.reader.readLine();
			log.finest("Received message \"" + msg + "\" from node " + theirId );
			return Message.fromString(msg);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e){
			log.finest("Failed to decode message \"" + msg + "\" from node " + theirId);
		}
		
		return null;

	}	
	
	
//****************************************************************
//	Private Methods -- mostly network related
//****************************************************************	
	
	/**
	 * Send a message to another node
	 */
	private void sendMessage(NodeInfo node, Message msg) {
		log.finest("Sending message \"" + msg + "\" to " + node );
		node.writer.println(msg.toString());
		node.writer.flush();
	}
	
	/**
	 * Receive a message from another node   
	 */
	private Message receiveMessage(NodeInfo node){
		String msg = null;
		try {
			msg = node.reader.readLine();
			log.finest("Received message \"" + msg + "\" from " + node );
			return Message.fromString(msg);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e){
			log.finest("Failed to decode message \"" + msg + "\" from " + node);
		}
		
		return null;

	}
	
	
	/**
	 * Private class to track node info. Just a struct. 
	 */
	static class NodeInfo{
		InetAddress address;
		int port;
		boolean connected;
		Socket sock;
		PrintWriter writer;
		BufferedReader reader;
		int connectionAttempts;
		
		public NodeInfo() {}
		
		public NodeInfo(Socket sock) {
			try{
				address = sock.getInetAddress();
				port = sock.getPort();
				writer = new PrintWriter(sock.getOutputStream());
				reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				connected = true;
			} catch(IOException e){
				connected = false;
			}
		}
		
		public int isConnected() {
			return connected ? 1 : 0;
		}
		
		@Override
		public String toString(){
			return address.toString() + ":" + port;
		}
	}
	
	
	/**
	 * Initialize Node connections, including listening for incoming connections
	 * And attempting to connect to other servers
	 */
	private void networkInit(){
		try {
			serverSocket = new ServerSocket(getPort()+1);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		startConnectionThread();
		connectToOtherServers(restart);
	}
	
	
	/**
	 * Start the infinite TCP listening thread
	 */
	private void startConnectionThread(){
		log.info("Initating connections");
		
		// start eternal socket acceptance loop
		connectThread = new Thread(new Runnable(){
			@Override
			public void run() {
				connectionLoop();
			}
		});
		connectThread.start();
	}
	
	
	/**
	 * Infinite TCP connection listener loop
	 */
	private void connectionLoop(){
		while(true){
			log.finer("Listenining for connection on port "+ (getPort()+1));
			try {
				// set up connection from unknown server
				Socket sock = serverSocket.accept();
				initIncomingConnection(sock);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * Initialize a new outgoing connection. 
	 */
	private void initIncomingConnection(Socket sock) throws IOException{
		NodeInfo node = new NodeInfo(sock);
		log.finer("Initializing incoming connection with node at " + node);
		
		// find out what id they are
		
		int theirId = 0;
		try{
			Message msg = receiveMessage(node);
			node.connected = true;
			
			if(msg.getType() != MessageType.INIT){
				log.warning("Received non INIT message from " + node);
				node.connected = false;
			}
			theirId = msg.getId();
			
			if(theirId < 0 || theirId >= nodes.size()){
				log.warning("Received out-of-bounds id from " + node);
				node.connected = false;
			}
			
			if(nodes.get(theirId).connected){
				if(node.address.equals(nodes.get(theirId).address)){
					log.info("Node " + theirId + " coming back online...");
				} else {
					log.warning("Received in-use id from " + node);
					node.connected = false;
				}
			}
			
		} catch(IllegalArgumentException e){
			log.warning("Could not decode message from " + node);
			node.connected = false;
		}
		
		if(!node.connected){
			Message nogood = new Message(MessageType.NACK, "reject", 0, id);
			sendMessage(node, nogood);
			sock.close();
			return;
		}
		
		// if ok, add to list of nodes and send ack
		nodes.set(theirId, node);
		Message ack_msg = new Message(MessageType.INIT, "ACK", 0, id); 
		sendMessage(node, ack_msg);
	}
	
	
	/**
	 * Connect to the other nodes. If "restart==false", this will attempt to
	 * connect only to servers with id < this.serverID. If "restart==true", this will
	 * attempt to connect to all other servers.  
	 */
	private void connectToOtherServers(boolean restart){
		// create other initial connections
		int iServer = -1;
		
		// if restarting, start by assuming that you must connect to all other servers
		// this will be updated after connecting to one live server
		// otherwise, on a fresh startup, connect to all servers with serverID less than yours 
		int nConnections = restart ? (nodes.size()) : id+1;
		
		while(getConnectedCount() < nConnections) {
			iServer = (iServer+1) % (restart?nConnections:id);
			NodeInfo node = nodes.get(iServer);
			if (node.connected)
				continue; // this socket and thread will be null

			try {
				log.fine("Entering connect loop for iServer = " + iServer);
				Socket sock = new Socket();
				try{
					sock.connect(new InetSocketAddress(node.address, node.port + 1), TIMEOUT);
					node.connected = true;
				} catch (ConnectException e) {
					// could not connect. 
					// Wait for an increasing amount of time, depending on number of attempts made
					// max wait time = 1 minute
					try {
						long waitTime = Math.min(node.connectionAttempts * WAIT_TIME, MAX_WAIT_TIME);  
						Thread.sleep(waitTime);
						node.connectionAttempts++;
					} catch (InterruptedException e1) {}
						log.finer("NetworkNode connection NOT made to server "+iServer);
					continue;
				}
				
				initOutgoingConnection(sock, iServer);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		log.fine("Finished in ctor connection loop");
	}

	
	/**
	 * Initialize a new incoming connection. 
	 */
	private void initOutgoingConnection(Socket sock, int iServer) throws IOException{
		NodeInfo node = new NodeInfo(sock);
		log.finer("Initializing outgoing connection with node at " + node);
		
		// send init message identifying myself
		Message msg = new Message(MessageType.INIT, "id", id, id);
		sendMessage(node, msg);
		
		// listen for ack message
		Message ack_msg = receiveMessage(node);
		
		nodes.set(ack_msg.getId(), node);
	}
	
	
//****************************************************************
//	main()
//****************************************************************
	
	/**
	 * Run the main program
	 * 
	 * @param args
	 *            command line input. Expects [id] [nodeList file] [optional "restart"]
	 *            
	 * Format of nodeList file:
	 * 127.0.0.1:8000
	 * 127.0.0.1:8005
	 * [ip]:[port]
	 * ...
	 */
	public static void main(String[] args) {
		if (args.length < 2 || args.length > 3) {
			System.out.println("ERROR: Provide 2 or 3 arguments");
			System.out.println("\t(1) <int>: process id, between 0 and number of nodes in node list file");
			System.out.println("\t(2) <file>: node list file");
			System.out.println("\t(3) <restart>: optional flag to restart failed server");
			System.exit(-1);
		}

		int id=0;
		try{
			id = Integer.parseInt(args[0]);
		} catch (NumberFormatException e){
			System.err.println("Error parsing process id from input string " + args[0]);
			System.exit(1);
		}
		
		String fileName = args[1];
		
		boolean restart = false;
		if(args.length==2 && args[1].equals("restart"))
			restart = true;

		File file = new File(fileName);
		if (!file.exists() || file.isDirectory()){
			System.err.println("IO error for nodeList file: " + fileName);
			System.exit(2);
		}


		NetworkNode node = new NetworkNode(id, fileName, restart);
		node.run();
		
	}

}
	
	
