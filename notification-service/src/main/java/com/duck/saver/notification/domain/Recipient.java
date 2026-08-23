package com.duck.saver.notification.domain;

/**
 * 邮件发送场景的接收人值对象。
 */
public class Recipient {

	private String accountName;

	private String email;

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
