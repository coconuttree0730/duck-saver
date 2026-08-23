package com.duck.saver.account.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class StatisticsServiceClientFallbackTest {

	private final StatisticsServiceClientFallback fallback = new StatisticsServiceClientFallback();

	@Test
	public void shouldSwallowErrorOnFallback() {
		assertDoesNotThrow(() -> fallback.updateStatistics("test", new com.duck.saver.account.client.dto.StatisticsPayload()));
	}
}
