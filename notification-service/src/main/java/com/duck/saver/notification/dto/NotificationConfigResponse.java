package com.duck.saver.notification.dto;

public class NotificationConfigResponse {

	private String type;

	private String cronExpression;

	public NotificationConfigResponse() {
	}

	public NotificationConfigResponse(String type, String cronExpression) {
		this.type = type;
		this.cronExpression = cronExpression;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCronExpression() {
		return cronExpression;
	}

	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}
}
