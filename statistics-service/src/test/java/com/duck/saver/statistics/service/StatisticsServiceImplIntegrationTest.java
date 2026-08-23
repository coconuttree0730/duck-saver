package com.duck.saver.statistics.service;

import com.duck.saver.statistics.domain.Account;
import com.duck.saver.statistics.dto.StatisticsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
class StatisticsServiceImplIntegrationTest {

	@Container
	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
			.withDatabaseName("duck_saver_statistics")
			.withInitScript("sql/statistics_schema.sql");

	@Autowired
	private StatisticsServiceImpl statisticsService;

	private Account payload(String incomeTitle, BigDecimal income, String expenseTitle, BigDecimal expense) {

		Account.Item in = new Account.Item();
		in.setTitle(incomeTitle);
		in.setAmount(income);

		Account.Item out = new Account.Item();
		out.setTitle(expenseTitle);
		out.setAmount(expense);

		Account account = new Account();
		account.setIncomes(List.of(in));
		account.setExpenses(List.of(out));

		Account.Saving saving = new Account.Saving();
		saving.setAmount(new BigDecimal("5000.00"));
		account.setSaving(saving);
		return account;
	}

	@Test
	public void shouldSaveAndReturnAggregate() {

		statisticsService.save("it-account", payload("工资", new BigDecimal("15000"), "午餐", new BigDecimal("48")));

		StatisticsResponse response = statisticsService.findByAccountName("it-account");

		assertEquals("it-account", response.getAccount());
		assertEquals(1, response.getCashflow().size());
		assertEquals(0, BigDecimal.valueOf(15000).compareTo(response.getIncome().getCurrentValue()));
		assertEquals(0, BigDecimal.valueOf(48).compareTo(response.getExpense().getCurrentValue()));
	}

	@Test
	public void shouldUpsertSameDayDatapoint() {

		statisticsService.save("upsert", payload("工资", new BigDecimal("100"), "咖啡", new BigDecimal("9")));
		statisticsService.save("upsert", payload("工资", new BigDecimal("200"), "咖啡", new BigDecimal("18")));

		StatisticsResponse response = statisticsService.findByAccountName("upsert");
		assertEquals(1, response.getCashflow().size());
		assertEquals(0, BigDecimal.valueOf(200).compareTo(response.getIncome().getCurrentValue()));
	}
}
