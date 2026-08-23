package com.duck.saver.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginResponse {

	private String tokenName;

	private String tokenValue;

	@JsonProperty("refresh_token")
	private String refreshToken;

	@JsonProperty("expires_in")
	private Long expiresIn;

	public LoginResponse() {
	}

	public LoginResponse(String tokenName, String tokenValue, String refreshToken, Long expiresIn) {
		this.tokenName = tokenName;
		this.tokenValue = tokenValue;
		this.refreshToken = refreshToken;
		this.expiresIn = expiresIn;
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

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public Long getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(Long expiresIn) {
		this.expiresIn = expiresIn;
	}
}
