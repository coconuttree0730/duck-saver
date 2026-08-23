package com.duck.saver.common.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * 账户快照：胖事件的 data 载荷，消费方据此直接落数据点，零回调 account-service。
 * 字段演进只增不删，保持向后兼容。
 */
public class AccountSnapshot {

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

		public Item() {
		}

		public Item(String title, BigDecimal amount) {
			this.title = title;
			this.amount = amount;
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
	}

	public static class Saving {

		private BigDecimal amount;

		public Saving() {
		}

		public Saving(BigDecimal amount) {
			this.amount = amount;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}
	}
}
