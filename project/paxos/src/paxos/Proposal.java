package paxos;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class Proposal {
	private static final Gson gson = new Gson();
	
	int number;
	String value;
	
	public Proposal(int number, String value) {
		this.number = number;
		this.value = value;
	}
	
	
	@Override
	public String toString() throws IllegalArgumentException{
		try{
			return gson.toJson(this);
		} catch(JsonSyntaxException e){
			throw new IllegalArgumentException("Could not encode Message from string");
		}
	}
	
	public static Proposal fromString(String string) throws IllegalArgumentException{
		try{
			return gson.fromJson(string, Proposal.class);
		} catch(JsonSyntaxException e){
			throw new IllegalArgumentException("Could not decode Message from string");
		}
	}
	
}
