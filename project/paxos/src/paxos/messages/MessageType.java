package paxos.messages;

public enum MessageType {
	PREPARE_REQUEST,
	PREPARE_RESPONSE,
	ACCEPT_REQUEST,
	ACCEPT_RESPONSE,
	NACK,
	INIT,
	APP
}
