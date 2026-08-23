package com.duck.saver.notification.dto;

import java.util.List;

public class RecipientResponse {

	private RecipientInfoResponse recipient;

	private List<NotificationConfigResponse> notificationConfig;

	public RecipientInfoResponse getRecipient() {
		return recipient;
	}

	public void setRecipient(RecipientInfoResponse recipient) {
		this.recipient = recipient;
	}

	public List<NotificationConfigResponse> getNotificationConfig() {
		return notificationConfig;
	}

	public void setNotificationConfig(List<NotificationConfigResponse> notificationConfig) {
		this.notificationConfig = notificationConfig;
	}
}
