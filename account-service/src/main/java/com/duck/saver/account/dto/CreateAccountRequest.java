package com.duck.saver.account.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateAccountRequest {

	@NotBlank
	private String name;

	@NotBlank
	private String currency;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}
}
