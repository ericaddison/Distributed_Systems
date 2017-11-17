package paxos.messages;

public enum MessageType {
	PROPOSE_REQUEST,
	PROPOSE_RESPONSE,
	ACCEPT_REQUEST,
	ACCEPT_RESPONSE,
	NACK,
	INIT,
	APP
}
