package store;

import java.io.Serializable;

public class ClientCancel implements Serializable {
	
	int orderID;
	
	public ClientCancel(int id) {
		orderID = id;
	}
	
	public ClientCancel(String id) {
		this(Integer.parseInt(id));
	}	

}
