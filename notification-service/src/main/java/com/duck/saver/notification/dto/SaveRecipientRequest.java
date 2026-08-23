package com.duck.saver.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class SaveRecipientRequest {

	@NotBlank
	@Email
	private String email;

	/** WEEKLY / MONTHLY / QUARTERLY，默认 WEEKLY */
	private String frequency;

	private Boolean enabled = true;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFrequency() {
		return frequency;
	}

	public void setFrequency(String frequency) {
		this.frequency = frequency;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
}
