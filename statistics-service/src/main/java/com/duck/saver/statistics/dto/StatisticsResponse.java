package com.duck.saver.statistics.dto;

import java.util.List;

public class StatisticsResponse {

	private String account;

	private MetricResponse expense;

	private MetricResponse income;

	private MetricResponse saving;

	private List<CashflowEntry> cashflow;

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public MetricResponse getExpense() {
		return expense;
	}

	public void setExpense(MetricResponse expense) {
		this.expense = expense;
	}

	public MetricResponse getIncome() {
		return income;
	}

	public void setIncome(MetricResponse income) {
		this.income = income;
	}

	public MetricResponse getSaving() {
		return saving;
	}

	public void setSaving(MetricResponse saving) {
		this.saving = saving;
	}

	public List<CashflowEntry> getCashflow() {
		return cashflow;
	}

	public void setCashflow(List<CashflowEntry> cashflow) {
		this.cashflow = cashflow;
	}
}
