package store;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class TcpServerTask implements Runnable {

	Socket clientSocket;
	Server server;

	public TcpServerTask(Server server, Socket clientSocket) {
		super();
		this.server = server;
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
					server.processClientOrder((ClientOrder) receivedObject, out);
				else if (receivedObject.getClass() == ClientCancel.class)
					server.processClientCancel((ClientCancel) receivedObject, out);
					
			}
		} catch (EOFException e){
			System.out.println("Connection to " + clientSocket.getInetAddress() + " ended unexpectedly.");
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		} 
	}
}