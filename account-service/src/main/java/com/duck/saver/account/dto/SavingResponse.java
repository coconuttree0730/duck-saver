package com.duck.saver.account.dto;

import java.math.BigDecimal;

public class SavingResponse {

	private BigDecimal amount;

	private BigDecimal interest;

	private BigDecimal deposit;

	private String currency;

	public SavingResponse() {
	}

	public SavingResponse(BigDecimal amount, BigDecimal interest, BigDecimal deposit, String currency) {
		this.amount = amount;
		this.interest = interest;
		this.deposit = deposit;
		this.currency = currency;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getInterest() {
		return interest;
	}

	public void setInterest(BigDecimal interest) {
		this.interest = interest;
	}

	public BigDecimal getDeposit() {
		return deposit;
	}

	public void setDeposit(BigDecimal deposit) {
		this.deposit = deposit;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}
}
