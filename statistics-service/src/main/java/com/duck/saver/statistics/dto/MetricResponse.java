package com.duck.saver.statistics.dto;

import java.math.BigDecimal;

public class MetricResponse {

	private BigDecimal currentValue;

	private BigDecimal previousValue;

	private BigDecimal percentChange;

	public MetricResponse() {
	}

	public MetricResponse(BigDecimal currentValue, BigDecimal previousValue, BigDecimal percentChange) {
		this.currentValue = currentValue;
		this.previousValue = previousValue;
		this.percentChange = percentChange;
	}

	public BigDecimal getCurrentValue() {
		return currentValue;
	}

	public void setCurrentValue(BigDecimal currentValue) {
		this.currentValue = currentValue;
	}

	public BigDecimal getPreviousValue() {
		return previousValue;
	}

	public void setPreviousValue(BigDecimal previousValue) {
		this.previousValue = previousValue;
	}

	public BigDecimal getPercentChange() {
		return percentChange;
	}

	public void setPercentChange(BigDecimal percentChange) {
		this.percentChange = percentChange;
	}
}
