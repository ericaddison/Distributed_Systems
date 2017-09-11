package store;

import java.io.Serializable;

public class ClientOrder implements Serializable {
	
	String userName;
	String productName;
	int quantity;
	boolean isActive;
	int orderID;
	
	public ClientOrder(String un, String pn, int q) {
		userName = un;
		productName = pn;
		quantity = q;
		isActive = true;
	}
	
	public String toString() {
		return orderID + ": " + userName + ": " + quantity + " * " + productName + " (" + (isActive?"active":"cancelled") + ")";
	}
	
}
