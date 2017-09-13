package store;

import java.io.Serializable;

public class ClientCancel implements Serializable {
	
	private static final long serialVersionUID = 1L;
	int orderID;
	
	public ClientCancel(int id) {
		orderID = id;
	}
	
	public ClientCancel(String id) {
		this(Integer.parseInt(id));
	}	

}
