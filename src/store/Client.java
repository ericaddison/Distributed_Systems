package store;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
	

	
	
	
  public static void main (String[] args) {
    String hostAddress;
    int tcpPort;
    int udpPort;

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
		in = new BufferedReader( new InputStreamReader(sock.getInputStream()));
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    
    
    
    Scanner sc = new Scanner(System.in);
    while(sc.hasNextLine()) {
      String cmd = sc.nextLine();
      String[] tokens = cmd.split(" ");

      if (tokens[0].equals("setmode")) {
        // TODO: set the mode of communication for sending commands to the server 
        // and display the name of the protocol that will be used in future
      }
      else if (tokens[0].equals("purchase")) {
        // TODO: send appropriate command to the server and display the
        // appropriate responses form the server
    	  
    	  // purchase <user-name> <product-name> <quantity>
    	  if (tokens.length < 4) {
    		  System.out.println("ERROR: Not enough tokens in purchase string");
    		  System.out.println("ERROR: Expected format: purchase <user-name> <product-name> <quantity>");
    	  }
    	  String userName = tokens[1];
    	  String productName = tokens[2];
    	  int quantity = Integer.parseInt(tokens[3]);
    	  System.out.println("Purchase order received: " + userName + " to purchase " + quantity + " " + productName + " items");
      	  
    	  ClientOrder order = new ClientOrder(userName, productName, quantity);
    	  try {
			out.writeObject(order);
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
      } else if (tokens[0].equals("list")) {
        // TODO: send appropriate command to the server and display the
        // appropriate responses form the server
      } else {
    	try {
			System.out.println(in.readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
        System.out.println("ERROR: No such command");
      }
    }
  }
}

