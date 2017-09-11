package store;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

	    // set up server tcp socket
	    try (
            ServerSocket serverSocket =
                new ServerSocket(Integer.parseInt(args[0]));
            Socket clientSocket = serverSocket.accept();
            PrintWriter out =
                new PrintWriter(clientSocket.getOutputStream(), true);                   
            BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
        ) {
	    	System.out.println("Accepted connection from " + clientSocket.getInetAddress());
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
            	System.out.println("Server received: " + inputLine);
                out.println("From server: " + inputLine);
            }
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                + tcpPort + " or listening for a connection");
            System.out.println(e.getMessage());
        }

	    
	    
	    // parse the inventory file

	    // TODO: handle request from clients
	  }
	}