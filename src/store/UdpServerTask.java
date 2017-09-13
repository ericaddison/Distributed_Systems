package store;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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


	@Override
	public void run() {
		DatagramPacket datapacket;
		try{
			while (true) {
				ObjectAndPacket oap = UdpIO.receiveObject(datasocket, 1024);
				datapacket = oap.datagramPacket;
				System.out.println("Received packet from " + datapacket.getAddress() + ": " + oap.object);
				UdpIO.sendObject(oap.object, oap.datagramPacket.getAddress(), oap.datagramPacket.getPort(), datasocket);
			}
		} catch (IOException e) {
			System.err.println(e);
			e.printStackTrace();
		}
		
	}
	
	
}
