package paxos.messages;

public enum MessageType {
	PROPOSE_REQUEST,
	ACCEPT_REQUEST,
	ACCEPT_RESPONSE,
	NACK
}
