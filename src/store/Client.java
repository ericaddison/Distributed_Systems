package store;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

	public static void main(String[] args) {

		String hostAddress;
		int tcpPort;
		int udpPort;
		boolean modeIsTCP = true; // TCP is default mode, other mode is UDP

		if (args.length != 3) {
			System.out.println("ERROR: Provide 3 arguments");
			System.out.println("\t(1) <hostAddress>: the address of the server");
			System.out.println("\t(2) <tcpPort>: the port number for TCP connection");
			System.out.println("\t(3) <udpPort>: the port number for UDP connection");
			System.exit(-1);
		}

		hostAddress = args[0];
		tcpPort = Integer.parseInt(args[1]);
		udpPort = Integer.parseInt(args[2]);

		// connect a socket
		// - out lets you send to the server
		// - in lets you receive from the server
		Socket sock = null;
		ObjectOutputStream out = null;
		BufferedReader in = null;
		try {
			sock = new Socket(hostAddress, tcpPort);
			out = new ObjectOutputStream(sock.getOutputStream());
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Scanner sc = new Scanner(System.in);
		while (sc.hasNextLine()) {
			String cmd = sc.nextLine();
			String[] tokens = cmd.split(" ");

			if (tokens[0].equals("setmode")) {
				// TODO: set the mode of communication for sending commands to
				// the server
				// and display the name of the protocol that will be used in
				// future

				// expecting setmode T | U
				if (tokens.length < 2) {
					System.out.println("ERROR: Not enough tokens in setmode string");
					System.out.println("ERROR: Expected format: setmode T | U");
				}

				String mode = tokens[1].toUpperCase();
				if (mode == "T") {
					modeIsTCP = true;
				} else if (mode == "U") {
					modeIsTCP = false;
				} else {
					// unrecognized mode, setting to TCP (since that is the
					// default mode)
					System.out.println("ERROR: unrecognized mode: " + mode + ", mode is set to TCP");
					modeIsTCP = true;
				}

				//
				//
				// TO DO: use modeIsTCP boolean to actually change the mode?
				//
				//

			} else if (tokens[0].equals("purchase")) {
				// TODO: send appropriate command to the server and display the
				// appropriate responses form the server

				// expected format: purchase <user-name> <product-name>
				// <quantity>
				if (tokens.length < 4) {
					System.out.println("ERROR: Not enough tokens in purchase string");
					System.out.println("ERROR: Expected format: purchase <user-name> <product-name> <quantity>");
				}
				String userName = tokens[1];
				String productName = tokens[2];
				int quantity = Integer.parseInt(tokens[3]);
				System.out.println("Purchase order received: " + userName + " to purchase " + quantity + " "
						+ productName + " items");

				ClientOrder order = new ClientOrder(userName, productName, quantity);
				try {
					out.writeObject(order);
					System.out.println(in.readLine());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else if (tokens[0].equals("cancel")) {
				// TODO: send appropriate command to the server and display the
				// appropriate responses form the server

				if (tokens.length < 3) {
					System.out.println("ERROR: Not enough tokens in cancel string");
					System.out.println("ERROR: Expected format: cancel <order-id>");
				}
				String orderID = tokens[1];

			} else if (tokens[0].equals("search")) {
				// TODO: send appropriate command to the server and display the
				// appropriate responses form the server

				if (tokens.length < 2) {
					System.out.println("ERROR: Not enough tokens in search string");
					System.out.println("ERROR: Expected format: search <user-name>");
				}
				String userName = tokens[1];

				//
				//
				// TO DO: send search(userName) request to server
				// server returns either
				// "No order found for <user-name>"
				// or, list all orders of the user as <orderId> <productName>
				// <quantity>
				//
				//
				//

			} else if (tokens[0].equals("list")) {
				// TODO: send appropriate command to the server and display the
				// appropriate responses form the server
				try {
					out.writeObject(new ClientProductList());
					String list = in.readLine().replace(":", "\n");
					System.out.println(list);
				} catch (IOException e) {
					e.printStackTrace();
				}

				
				//
				//
				// TO DO: send list request to server
				//
				// return list <productName> <quantity>
				// print one line per product (if product sold out, list
				// quantity as 0)
				// products should be listed in sorted order

			} else {

				System.out.println("ERROR: No such command");

			}
			
		}
	}
}
