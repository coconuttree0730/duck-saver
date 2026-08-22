package com.duck.saver.account.domain;

public enum Currency {

	USD, EUR, RUB;

	public static Currency getDefault() {
		return USD;
	}
}
