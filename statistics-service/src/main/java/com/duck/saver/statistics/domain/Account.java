package com.duck.saver.statistics.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * 账户服务经 Feign 推送的内部负载（与 account-service 的 StatisticsPayload 结构一致）。
 */
public class Account {

	private List<Item> incomes;

	private List<Item> expenses;

	private Saving saving;

	public List<Item> getIncomes() {
		return incomes;
	}

	public void setIncomes(List<Item> incomes) {
		this.incomes = incomes;
	}

	public List<Item> getExpenses() {
		return expenses;
	}

	public void setExpenses(List<Item> expenses) {
		this.expenses = expenses;
	}

	public Saving getSaving() {
		return saving;
	}

	public void setSaving(Saving saving) {
		this.saving = saving;
	}

	public static class Item {

		private String title;

		private BigDecimal amount;

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
	}

	public static class Saving {

		private BigDecimal amount;

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}
	}
}
