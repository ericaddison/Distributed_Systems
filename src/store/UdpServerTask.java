package store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UdpServerTask implements Runnable{

	private DatagramSocket datasocket;
	
	public UdpServerTask(int udpPort) {
		super();
		try {
			this.datasocket = new DatagramSocket(udpPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}


	public static ObjectAndPacket receiveObject(DatagramSocket sock) throws IOException{
		int len = 1024;
		DatagramPacket datapacket = new DatagramPacket(new byte[len], len);
		sock.receive(datapacket);
		
		ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(datapacket.getData()));
		Object o = null;
		try {
			o = is.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return new ObjectAndPacket(o, datapacket);
	}
	
	
	
	public static void sendObject(Object o, InetAddress addr, int port, DatagramSocket sock) throws IOException{
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(bos);
		os.writeObject(o);
		byte[] barray = bos.toByteArray();
		
		DatagramPacket returnpacket = new DatagramPacket(
				barray,
				barray.length,
				addr,
				port);
		sock.send(returnpacket);
		
	}


	@Override
	public void run() {
		DatagramPacket datapacket;
		try{
			while (true) {
				ObjectAndPacket oap = receiveObject(datasocket);
				datapacket = oap.datagramPacket;
				System.out.println("Received packet from " + datapacket.getAddress() + ": " + oap.object);
				sendObject(oap.object, oap.datagramPacket.getAddress(), oap.datagramPacket.getPort(), datasocket);
			}
		} catch (IOException e) {
			System.err.println(e);
			e.printStackTrace();
		}
		
	}
	
	
}
