package store;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	
	private int tcpPort;
	private int udpPort;
	private Inventory inv;
	private OrderHistory orders;
	
	
	
	  public Server(int tcpPort, int udpPort, String fileName) {
		super();
		this.tcpPort = tcpPort;
		this.udpPort = udpPort;
		inv = new Inventory(fileName);
		orders = new OrderHistory();
	}

	public void run() {
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
            	if(receivedObject.getClass()==ClientOrder.class)
            		processClientOrder((ClientOrder)receivedObject, out);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Exception caught when trying to listen on port "
                + tcpPort + " or listening for a connection");
            System.out.println(e.getMessage());
        }

	    
	    // END SAMPLE CODE
	    
	    


	    // TODO: handle request from clients
	  }
	  
	
	
	  public void processClientOrder(ClientOrder order, PrintWriter out) {
		  StringBuilder response = new StringBuilder();
		  int stock = inv.getItemCount(order.productName);
		  int quantity = order.quantity;
		  
		  if(stock==-1)
			  response.append("Not Available - We do not sell this product");
		  else if(stock<quantity)
			  response.append("Not Available - Not enough items");
		  else {
			  orders.addOrder(order);
			  inv.removeItem(order.productName, quantity);
			  response.append("Your order has been placed, ");
			  response.append(order.orderID);
			  response.append(" ");
			  response.append(order.userName);
			  response.append(" ");
			  response.append(order.productName);
			  response.append(" ");
			  response.append(order.quantity);
		  }
		  out.println(response);
	  }
	  
	  
	  
	  
	  public static void main (String[] args){
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
		    
		    Server server = new Server(tcpPort, udpPort, fileName);
		    server.run();
	  }
	}