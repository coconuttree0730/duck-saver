package com.duck.saver.account.controller;

import com.duck.saver.account.dto.AccountResponse;
import com.duck.saver.account.dto.TransactionItemRequest;
import com.duck.saver.account.dto.ItemResponse;
import com.duck.saver.account.dto.SavingResponse;
import com.duck.saver.account.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.security.auth.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountControllerTest {

	private static final ObjectMapper mapper = new ObjectMapper();

	@InjectMocks
	private AccountController accountController;

	@Mock
	private AccountService accountService;

	private MockMvc mockMvc;

	@BeforeEach
	public void setup() throws Exception {
		MockitoAnnotations.openMocks(this).close();
		this.mockMvc = MockMvcBuilders.standaloneSetup(accountController)
				.setControllerAdvice(new com.duck.saver.common.web.GlobalExceptionHandler(),
						new com.duck.saver.common.web.ResultWrapAdvice(mapper))
				.build();
	}

	@Test
	public void shouldGetCurrentAccount() throws Exception {

		AccountResponse response = stubAccount("test");

		when(accountService.findByName("test")).thenReturn(response);

		mockMvc.perform(get("/accounts/current").principal(new UserPrincipal("test")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.name").value("test"))
				.andExpect(jsonPath("$.data.items[0].category").value("餐饮"))
				.andExpect(jsonPath("$.data.saving.amount").value(10000.00));
	}

	@Test
	public void shouldCreateAccount() throws Exception {

		AccountResponse response = stubAccount("new-account");
		when(accountService.create(any())).thenReturn(response);

		String json = """
				{ "name": "new-account", "currency": "CNY" }
				""";

		mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("new-account"));
	}

	@Test
	public void shouldReturn400WhenCreateWithoutName() throws Exception {

		String json = "{ \"currency\": \"CNY\" }";

		mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(400));
	}

	@Test
	public void shouldAddTransactionItem() throws Exception {

		String json = """
				{ "title": "午餐", "amount": 48.00, "currency": "CNY",
				  "category": "餐饮", "type": "EXPENSE", "date": "2026-08-23" }
				""";

		mockMvc.perform(post("/accounts/demo/items").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk());

		verify(accountService, times(1)).addItem(eq("demo"), any(TransactionItemRequest.class));
	}

	@Test
	public void shouldDeleteTransactionItem() throws Exception {

		mockMvc.perform(delete("/accounts/demo/items/some-uuid"))
				.andExpect(status().isOk());

		verify(accountService, times(1)).deleteItem("demo", "some-uuid");
	}

	@Test
	public void shouldUpdateAndDeleteAccount() throws Exception {

		mockMvc.perform(put("/accounts/test").contentType(MediaType.APPLICATION_JSON).content("{ \"currency\": \"USD\" }"))
				.andExpect(status().isOk());
		verify(accountService, times(1)).update(eq("test"), any());

		mockMvc.perform(delete("/accounts/test")).andExpect(status().isOk());
		verify(accountService, times(1)).delete("test");
	}

	private AccountResponse stubAccount(String name) {

		ItemResponse item = new ItemResponse("t-1", "午餐", new BigDecimal("48.00"), "CNY", "餐饮", "EXPENSE",
				LocalDate.now());
		SavingResponse saving = new SavingResponse(new BigDecimal("10000.00"), new BigDecimal("0.0150"),
				new BigDecimal("5000.00"), "CNY");

		AccountResponse response = new AccountResponse();
		response.setName(name);
		response.setCurrency("CNY");
		response.setLastUpdate(java.time.LocalDateTime.now());
		response.setItems(List.of(item));
		response.setSaving(saving);
		return response;
	}
}
