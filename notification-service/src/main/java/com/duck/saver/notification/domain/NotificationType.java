package com.duck.saver.notification.domain;

public enum NotificationType {

	BACKUP("backup.email.subject", "backup.email.text", "backup.email.attachment"),
	BILL_REMINDER("remind.email.subject", "remind.email.text", null);

	private final String subject;
	private final String text;
	private final String attachment;

	NotificationType(String subject, String text, String attachment) {
		this.subject = subject;
		this.text = text;
		this.attachment = attachment;
	}

	public String getSubject() {
		return subject;
	}

	public String getText() {
		return text;
	}

	public String getAttachment() {
		return attachment;
	}
}
