package store;

import java.io.EOFException;
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

		// start async udp responder here

		try (ServerSocket serverSocket = new ServerSocket(tcpPort);) {
			while (true) {
				Socket clientSocket = serverSocket.accept();
				Thread t = new Thread(new TcpServerTask(clientSocket));
				t.start();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		// END SAMPLE CODE

		// TODO: handle request from clients
	}

	public void processClientOrder(ClientOrder order, PrintWriter out) {
		StringBuilder response = new StringBuilder();
		int stock = inv.getItemCount(order.productName);
		int quantity = order.quantity;

		if (stock == -1)
			response.append("Not Available - We do not sell this product");
		else if (stock < quantity)
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

	public void processClientCancel(ClientCancel cancel, PrintWriter out) {

		ClientOrder order = orders.cancelOrderByID(cancel.orderID);

		if (order != null) {
			inv.addItem(order.productName, order.quantity);
			out.println("Order " + cancel.orderID + " is cancelled");
		} else
			out.println(cancel.orderID + " not found, no such order");

	}

	
	
	
	private class TcpServerTask implements Runnable {

		Socket clientSocket;

		public TcpServerTask(Socket clientSocket) {
			super();
			this.clientSocket = clientSocket;
		}

		@Override
		public void run() {
			try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());){

				System.out.println("Accepted connection from " + clientSocket.getInetAddress());
		
				Object receivedObject;
				while ((receivedObject = in.readObject()) != null) {
					System.out.println("Server received: " + receivedObject.getClass());
					if (receivedObject.getClass() == ClientOrder.class)
						processClientOrder((ClientOrder) receivedObject, out);
					else if (receivedObject.getClass() == ClientCancel.class)
						processClientCancel((ClientCancel) receivedObject, out);
						
				}
			} catch (EOFException e){
				System.out.println("Connection to " + clientSocket.getInetAddress() + " ended unexpectedly.");
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			} 
		}
	}

	
	
	
		private class UdpServerTask implements Runnable {

			@Override
			public void run() {

			}

		}

		public static void main(String[] args) {
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