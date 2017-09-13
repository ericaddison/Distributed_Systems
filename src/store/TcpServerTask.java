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
				ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());) {

			System.out.println("Accepted connection from " + clientSocket.getInetAddress());

			Object receivedObject;
			while ((receivedObject = in.readObject()) != null) {
				System.out.println("Server received: " + receivedObject.getClass());

				String response = "Unkown or bad command received";

				server.logInfo("Received " + receivedObject.getClass().getCanonicalName() + " request from "
						+ clientSocket.getInetAddress());

				if (receivedObject.getClass() == ClientOrder.class)
					response = server.processRequest((ClientOrder) receivedObject);

				else if (receivedObject.getClass() == ClientCancel.class)
					response = server.processRequest((ClientCancel) receivedObject);

				else if (receivedObject.getClass() == ClientSearch.class)
					response = server.processRequest((ClientSearch) receivedObject);

				else if (receivedObject.getClass() == ClientProductList.class)
					response = server.processRequest((ClientProductList) receivedObject);

				out.println(response);
			}
		} catch (EOFException e) {
			server.logWarn("Connection to " + clientSocket.getInetAddress() + " ended unexpectedly.");
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}