package store;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {
	String hostAddress;
	int tcpPort;
	int udpPort;
	boolean modeIsTCP = false; // TCP is default mode, other mode is UDP
	Socket tcpSocket = null;
	ObjectOutputStream out = null;
	BufferedReader in = null;
	DatagramSocket udpSocket;
	
	
	
	public Client(String hostAddress, int tcpPort, int udpPort) {
		super();
		this.hostAddress = hostAddress;
		this.tcpPort = tcpPort;
		this.udpPort = udpPort;
	}



	public static void main(String[] args) {

		if (args.length != 3) {
			System.out.println("ERROR: Provide 3 arguments");
			System.out.println("\t(1) <hostAddress>: the address of the server");
			System.out.println("\t(2) <tcpPort>: the port number for TCP connection");
			System.out.println("\t(3) <udpPort>: the port number for UDP connection");
			System.exit(-1);
		}

		String hostAddress = args[0];
		int tcpPort = Integer.parseInt(args[1]);
		int udpPort = Integer.parseInt(args[2]);
		
		Client client = new Client(hostAddress, tcpPort, udpPort);
		
		client.run();
	}
	
	public void connectTCP(){
		if(modeIsTCP)
			return;
		
		modeIsTCP = true;
		try {
			tcpSocket = new Socket(hostAddress, tcpPort);
			out = new ObjectOutputStream(tcpSocket.getOutputStream());
			in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void run(){

		// connect a socket
		// - out lets you send to the server
		// - in lets you receive from the server

		connectTCP();

		Scanner sc = new Scanner(System.in);
		while (sc.hasNextLine()) {
			String cmd = sc.nextLine();
			String[] tokens = cmd.split(" ");

			if (tokens[0].equals("setmode")) {
	
				// expecting setmode T | U
				if (tokens.length < 2) {
					System.out.println("ERROR: Not enough tokens in setmode string");
					System.out.println("ERROR: Expected format: setmode T | U");
				} else {

					String mode = tokens[1].toUpperCase();
					if (mode == "T") {
						connectTCP();
						System.out.println("mode: TCP");
					} else if (mode == "U") {
						modeIsTCP = false;
						try {
							tcpSocket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						System.out.println("mode: UDP");
					} else {
						// unrecognized mode, setting to TCP (since that is the default mode)
						System.out.println("ERROR: unrecognized mode: " + mode);
						System.out.println("mode: TCP");
						connectTCP();
					}
				}

			} else if (tokens[0].equals("purchase")) {
	
				if (tokens.length < 4) {
					System.out.println("ERROR: Not enough tokens in purchase string");
					System.out.println("ERROR: Expected format: purchase <user-name> <product-name> <quantity>");
				} else {
					String userName = tokens[1];
					String productName = tokens[2];
					int quantity = Integer.parseInt(tokens[3]);
					System.out.println("Purchase order received: " + userName + " to purchase " + quantity + " "
							+ productName + " items");
	
					ClientOrder order = new ClientOrder(userName, productName, quantity);
					sendObject(order);
					System.out.println(receiveString());
				}
			} else if (tokens[0].equals("cancel")) {
		
				if (tokens.length < 2) {
					System.out.println("ERROR: Not enough tokens in cancel string");
					System.out.println("ERROR: Expected format: cancel <order-id>");
				} else {
					String orderID = tokens[1];
					
					sendObject(new ClientCancel(orderID));
					String cancelConf = receiveString().replace(":", "\n");
					System.out.println(cancelConf);
				}
				

			} else if (tokens[0].equals("search")) {
		
				if (tokens.length < 2) {
					System.out.println("ERROR: Not enough tokens in search string");
					System.out.println("ERROR: Expected format: search <user-name>");
				} else {
					String userName = tokens[1];
	
					sendObject(new ClientSearch(userName));
					String orders = receiveString().replace(":", "\n");
					System.out.println(orders);
				}

			} else if (tokens[0].equals("list")) {

				sendObject(new ClientProductList());
				String list = receiveString().replace(":", "\n");
				System.out.println(list);


			} else {

				System.out.println("ERROR: No such command");

			}
			
		}
	}
	
	public void sendObject(Object o){
		
		try {
			if(modeIsTCP)
				out.writeObject(o);
			else
				UdpIO.sendObject(o, InetAddress.getByName(hostAddress), udpPort, udpSocket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String receiveString() {
		try {
			if (modeIsTCP) {
				return in.readLine();
			} else {
				return (String) UdpIO.receiveObject(udpSocket, 1024).object;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return "could not receive string";
	}
	
	
}
