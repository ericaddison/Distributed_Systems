package paxos;

import java.io.BufferedReader;
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

/**
 * Base class to build on. This class will only worry about creating and managing TCP
 * connections to other nodes. Algorithmic pieces will act as decorators on this base class. 
 * @author eyms
 *
 */
public class NetworkNode {

	private static final int TIMEOUT = 500;
	private static final long WAIT_TIME = 500;
	private int port;
	private int id;
	private List<Integer> connectedNodes;
	private List<InetAddress> otherNodes;
	private List<Integer> ports;
	private Thread connectThread;
	private ServerSocket serverSocket;
	private boolean restart;
	
	
	// Logging
	private final Logger log = Logger.getLogger(this.getClass().getCanonicalName());
	private Level logLevel = Level.ALL;
	
	
	
	public NetworkNode(int port, int id, List<InetAddress> otherNodes) {
		this.otherNodes = otherNodes;
		this.port = port;
		setupLogger();
		networkInit();
	}
	
	public int getPort(){
		return port;
	}
	
	public Logger getLogger(){
		return log;
	}

	
	private void setupLogger(){
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
	

//****************************************************************
//	Private Methods -- mostly network related
//****************************************************************	
	
	
	/**
	 * Initialize Lamport connections, including listening for incoming connections
	 * And attempting to connect to other servers
	 */
	private void networkInit(){
		try {
			serverSocket = new ServerSocket(port+1);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		connectedNodes = new ArrayList<>();
		connectedNodes.add(id);
		startConnectionThread();
		connectToOtherServers(restart, otherNodes.size());
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
			log.finer("Listenining for connection on port "+ (port+1));
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
		log.finer("Initializing connection with new server at " + sock.getInetAddress() + ":" + sock.getPort());
		
		PrintWriter pw = new PrintWriter(sock.getOutputStream());
		BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		

	}
	
	
	
	/**
	 * Connect to the other Lamport servers. If "restart==false", this will attempt to
	 * connect only to servers with id < this.serverID. If "restart==true", this will
	 * attempt to connect to all other servers.  
	 */
	private void connectToOtherServers(boolean restart, int nOthers){
		// create other initial connections
		int iServer = -1;
		
		// if restarting, start by assuming that you must connect to all other servers
		// this will be updated after connecting to one live server
		// otherwise, on a fresh startup, connect to all servers with serverID less than yours 
		int nConnections = restart ? (nOthers) : id+1;
		
		while(connectedNodes.size() < nConnections) {
			iServer = (iServer+1) % (restart?nConnections:id);
			if (connectedNodes.contains(iServer))
				continue; // this socket and thread will be null

			try {
				log.fine("Entering connect loop for iServer = " + iServer);
				Socket sock = new Socket();
				try{
					sock.connect(new InetSocketAddress(otherNodes.get(iServer), ports.get(iServer) + 1), TIMEOUT);
					connectedNodes.add(iServer);
				} catch (ConnectException e) {
					// could not connect. Wait 1/2 second and move on
					try {
						Thread.sleep(WAIT_TIME);
					} catch (InterruptedException e1) {}
					log.finer("Lamport connection NOT made to server "+iServer);
					continue;
				}
				
				nConnections = initOutgoingConnection(sock, iServer, nConnections);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		log.fine("Finished in ctor connection loop");
	}

	
	/**
	 * Initialize a new incoming connection. 
	 */
	private int initOutgoingConnection(Socket sock, int iServer, int nConnections) throws IOException{
		log.finer("Initializing connection with server " + iServer);

		PrintWriter pw = new PrintWriter(sock.getOutputStream());
		BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		
		return nConnections;
	}
	
	
	
}
