package paxos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class PaxosState {
	private static final Gson gson = new Gson();
	
	// General fields
	int id;
	int Nprocs;
	float[] acceptorWeights;
	List<Integer> distinguishedLearners;
	boolean distinguishedProposer = false;
	
	// Proposer fields
	int lastProposalNumber = -1;
	float prepareResponseSum = 0;
	float nackSum = 0;
	Proposal receivedProposal;
	
	// Acceptor fields
	Proposal acceptedProposal = null;
	int promiseNumber = -1;
	
	// Learner fields
	Proposal[] acceptedProposals;
	String myValue;
	String chosenValue;
	
	
	@Override
	public String toString() throws IllegalArgumentException{
		try{
			return gson.toJson(this);
		} catch(JsonSyntaxException e){
			throw new IllegalArgumentException("Could not encode PaxosState from string");
		}
	}
	
	public static PaxosState fromString(String string) throws IllegalArgumentException{
		try{
			return gson.fromJson(string, PaxosState.class);
		} catch(JsonSyntaxException e){
			throw new IllegalArgumentException("Could not decode PaxosState from string");
		}
	}
	
	
	public void writeToFile(){
		File stateFile = new File("./states/node " + id + ".state");
		try (PrintWriter pw = new PrintWriter(stateFile);){
			pw.println(this.toString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static PaxosState readFromFile(String filename){
		File stateFile = new File(filename);
		try (BufferedReader br = new BufferedReader(new FileReader(stateFile));){
			return fromString(br.readLine());
		} catch (IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}	
		return null;
	}
	
}
