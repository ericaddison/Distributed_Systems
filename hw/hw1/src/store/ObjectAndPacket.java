package store;

import java.net.DatagramPacket;

public class ObjectAndPacket {
	public Object object;
	public DatagramPacket datagramPacket;

	public ObjectAndPacket(Object object, DatagramPacket datagramPacket) {
		super();
		this.object = object;
		this.datagramPacket = datagramPacket;
	}

}
