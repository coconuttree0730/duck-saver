package com.duck.saver.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CashflowEntry {

	private LocalDate date;

	private BigDecimal income;

	private BigDecimal expense;

	private BigDecimal saving;

	public CashflowEntry() {
	}

	public CashflowEntry(LocalDate date, BigDecimal income, BigDecimal expense, BigDecimal saving) {
		this.date = date;
		this.income = income;
		this.expense = expense;
		this.saving = saving;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public BigDecimal getIncome() {
		return income;
	}

	public void setIncome(BigDecimal income) {
		this.income = income;
	}

	public BigDecimal getExpense() {
		return expense;
	}

	public void setExpense(BigDecimal expense) {
		this.expense = expense;
	}

	public BigDecimal getSaving() {
		return saving;
	}

	public void setSaving(BigDecimal saving) {
		this.saving = saving;
	}
}
