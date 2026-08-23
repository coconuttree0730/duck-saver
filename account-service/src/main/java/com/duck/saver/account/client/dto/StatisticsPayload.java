package com.duck.saver.account.client.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 推送给 statistics-service 的内部负载（与统计服务反序列化结构保持一致）。
 */
public class StatisticsPayload {

	private List<ItemPayload> incomes;

	private List<ItemPayload> expenses;

	private SavingPayload saving;

	public List<ItemPayload> getIncomes() {
		return incomes;
	}

	public void setIncomes(List<ItemPayload> incomes) {
		this.incomes = incomes;
	}

	public List<ItemPayload> getExpenses() {
		return expenses;
	}

	public void setExpenses(List<ItemPayload> expenses) {
		this.expenses = expenses;
	}

	public SavingPayload getSaving() {
		return saving;
	}

	public void setSaving(SavingPayload saving) {
		this.saving = saving;
	}

	public record ItemPayload(String title, BigDecimal amount) {
	}

	public record SavingPayload(BigDecimal amount) {
	}
}
