package store;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	  public static void main (String[] args) {
	    int tcpPort;
	    int udpPort;
	    if (args.length != 3) {
	      System.out.println("ERROR: Provide 3 arguments");
	      System.out.println("\t(1) <tcpPort>: the port number for TCP connection");
	      System.out.println("\t(2) <udpPort>: the port number for UDP connection");
	      System.out.println("\t(3) <file>: the file of inventory");

	      System.exit(-1);
	    }
	    tcpPort = Integer.parseInt(args[0]);
	    udpPort = Integer.parseInt(args[1]);
	    String fileName = args[2];

	    // parse the inventory file
	    Inventory inv = new Inventory(fileName);
	    
	    
	    
	    // SAMPLE TCP CODE
	    
	    // set up server tcp socket
	    // remember need to multi-thread
	    try (
            ServerSocket serverSocket = new ServerSocket(tcpPort);
            Socket clientSocket = serverSocket.accept();
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);                   
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
        ) {
	    	System.out.println("Accepted connection from " + clientSocket.getInetAddress());
            Object receivedObject;
            while ((receivedObject = in.readObject()) != null) {
            	System.out.println("Server received: " + receivedObject.getClass());
            	if(receivedObject.getClass()==ClientOrder.class){
            		ClientOrder co = (ClientOrder)receivedObject;
            		System.out.println("Purchase order received: " + co.userName + " to purchase " + co.quantity + " " + co.productName + " items");
            	}
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Exception caught when trying to listen on port "
                + tcpPort + " or listening for a connection");
            System.out.println(e.getMessage());
        }

	    
	    // END SAMPLE CODE
	    
	    


	    // TODO: handle request from clients
	  }
	}