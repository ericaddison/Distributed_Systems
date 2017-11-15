package paxos.messages;

import com.google.gson.Gson;

public class Message {
	private static final Gson gson = new Gson();
	private MessageType type;
	private String value;
	private int number;
	
	public Message(MessageType type, String value, int number) {
		super();
		this.type = type;
		this.value = value;
		this.number = number;
	}
	
	public MessageType getType() {
		return type;
	}

	public String getValue() {
		return value;
	}

	public int getNumber() {
		return number;
	}

	@Override
	public String toString(){
		return gson.toJson(this);
	}
	public static Message fromString(String string){
		return gson.fromJson(string, Message.class);
	}
	
	public static void main(String[] args) {
		
		Message msg = new Message(MessageType.ACCEPT_REQUEST, "my value", 10);
		System.out.println(msg);
		
		String json = msg.toString();
		System.out.println(json);
		
		Message msg2 = Message.fromString(json);
		System.out.println(msg2);
		
	}
	
}
