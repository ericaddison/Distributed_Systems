package store;

import java.io.Serializable;

public class ClientOrder implements Serializable {
	
	String userName;
	String productName;
	int quantity;
	
	public ClientOrder(String un, String pn, int q) {
		userName = un;
		productName = pn;
		quantity = q;
	}
	
	
}
