package com.duck.saver.statistics.client;

import com.duck.saver.statistics.domain.Currency;
import com.duck.saver.statistics.domain.ExchangeRatesContainer;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExchangeRatesClientFallbackTest {

	private final ExchangeRatesClientFallback fallback = new ExchangeRatesClientFallback();

	@Test
	public void shouldProvideEmptyRatesOnFallback() {

		ExchangeRatesContainer container = fallback.getRates(Currency.getBase());

		assertEquals(Currency.getBase(), container.getBase());
		assertNotNull(container.getRates());
		assertEquals(Collections.emptyMap(), container.getRates());
	}
}
