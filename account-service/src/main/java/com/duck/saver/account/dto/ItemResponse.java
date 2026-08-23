package com.duck.saver.account.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ItemResponse {

	private String id;

	private String title;

	private BigDecimal amount;

	private String currency;

	private String category;

	private String type;

	private LocalDate date;

	public ItemResponse() {
	}

	public ItemResponse(String id, String title, BigDecimal amount, String currency, String category, String type,
			LocalDate date) {
		this.id = id;
		this.title = title;
		this.amount = amount;
		this.currency = currency;
		this.category = category;
		this.type = type;
		this.date = date;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}
}
