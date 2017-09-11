package store;

import java.io.Serializable;

public class ClientCancel implements Serializable {
	
	String orderID;
	
	public ClientCancel(String id) {
		orderID = id;
	}

}
