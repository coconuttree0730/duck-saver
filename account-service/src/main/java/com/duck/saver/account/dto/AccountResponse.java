package com.duck.saver.account.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AccountResponse {

	private String name;

	private String currency;

	private LocalDateTime lastUpdate;

	private List<ItemResponse> items;

	private SavingResponse saving;

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

	public LocalDateTime getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(LocalDateTime lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public List<ItemResponse> getItems() {
		return items;
	}

	public void setItems(List<ItemResponse> items) {
		this.items = items;
	}

	public SavingResponse getSaving() {
		return saving;
	}

	public void setSaving(SavingResponse saving) {
		this.saving = saving;
	}
}
