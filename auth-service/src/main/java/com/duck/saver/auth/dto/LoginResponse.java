package com.duck.saver.auth.dto;

public class LoginResponse {

	private String tokenName;

	private String tokenValue;

	public LoginResponse() {
	}

	public LoginResponse(String tokenName, String tokenValue) {
		this.tokenName = tokenName;
		this.tokenValue = tokenValue;
	}

	public String getTokenName() {
		return tokenName;
	}

	public void setTokenName(String tokenName) {
		this.tokenName = tokenName;
	}

	public String getTokenValue() {
		return tokenValue;
	}

	public void setTokenValue(String tokenValue) {
		this.tokenValue = tokenValue;
	}
}
