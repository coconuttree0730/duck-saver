package com.duck.saver.common.event;

/**
 * 账户事件消息体（AGENTS.md 定稿）：必含 eventId(uuid)/eventType/timestamp/data。
 * 消费方按 eventId 幂等去重。
 */
public class AccountEvent {

	private String eventId;
	private EventType eventType;
	private long timestamp;
	private String accountName;
	private AccountSnapshot data;

	public AccountEvent() {
	}

	public AccountEvent(String eventId, EventType eventType, long timestamp, String accountName, AccountSnapshot data) {
		this.eventId = eventId;
		this.eventType = eventType;
		this.timestamp = timestamp;
		this.accountName = accountName;
		this.data = data;
	}

	public static AccountEvent of(EventType eventType, String accountName, AccountSnapshot data) {
		return new AccountEvent(java.util.UUID.randomUUID().toString(), eventType, System.currentTimeMillis(),
				accountName, data);
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public EventType getEventType() {
		return eventType;
	}

	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public AccountSnapshot getData() {
		return data;
	}

	public void setData(AccountSnapshot data) {
		this.data = data;
	}
}
