package com.duck.saver.statistics.controller;

import com.duck.saver.statistics.domain.Account;
import com.duck.saver.statistics.dto.StatisticsResponse;
import com.duck.saver.statistics.service.StatisticsService;

import static org.mockito.ArgumentMatchers.any;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatisticsControllerTest {

	@InjectMocks
	private StatisticsController statisticsController;

	@Mock
	private StatisticsService statisticsService;

	private MockMvc mockMvc;

	@BeforeEach
	public void setup() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		this.mockMvc = MockMvcBuilders.standaloneSetup(statisticsController)
				.setControllerAdvice(new com.duck.saver.common.web.GlobalExceptionHandler(),
						new com.duck.saver.common.web.ResultWrapAdvice(new com.fasterxml.jackson.databind.ObjectMapper()))
				.build();
	}

	@Test
	public void shouldGetAggregateByAccountName() throws Exception {

		StatisticsResponse response = new StatisticsResponse();
		response.setAccount("test");
		response.setExpense(new com.duck.saver.statistics.dto.MetricResponse(
				new BigDecimal("48"), BigDecimal.ZERO, BigDecimal.ZERO));
		response.setIncome(new com.duck.saver.statistics.dto.MetricResponse(
				new BigDecimal("15000"), BigDecimal.ZERO, BigDecimal.ZERO));
		response.setSaving(new com.duck.saver.statistics.dto.MetricResponse(
				new BigDecimal("5000"), BigDecimal.ZERO, BigDecimal.ZERO));
		response.setCashflow(List.of(new com.duck.saver.statistics.dto.CashflowEntry(
				LocalDate.now(), new BigDecimal("15000"), new BigDecimal("48"), new BigDecimal("5000"))));

		org.mockito.Mockito.when(statisticsService.findByAccountName("test")).thenReturn(response);

		mockMvc.perform(get("/test").principal(new com.sun.security.auth.UserPrincipal("test")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.account").value("test"))
				.andExpect(jsonPath("$.data.expense.currentValue").value(48))
				.andExpect(jsonPath("$.data.cashflow[0].income").value(15000));
	}

	@Test
	public void shouldSaveAccountStatistics() throws Exception {

		String json = """
				{ "incomes": [ { "title": "Salary", "amount": 9100 } ],
				  "expenses": [ { "title": "Grocery", "amount": 10 } ],
				  "saving": { "amount": 1500 } }
				""";

		mockMvc.perform(put("/test").contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk());

		verify(statisticsService, times(1)).save(org.mockito.ArgumentMatchers.eq("test"), any(Account.class));
	}
}
