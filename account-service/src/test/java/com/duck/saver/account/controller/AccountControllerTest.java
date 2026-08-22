package com.duck.saver.account.controller;

import com.duck.saver.account.domain.Account;
import com.duck.saver.account.domain.Currency;
import com.duck.saver.account.domain.Item;
import com.duck.saver.account.domain.Saving;
import com.duck.saver.account.domain.TimePeriod;
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
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
				.setControllerAdvice(new com.duck.saver.common.web.GlobalExceptionHandler(), new com.duck.saver.common.web.ResultWrapAdvice(mapper))
				.build();
	}

	@Test
	public void shouldGetAccountByName() throws Exception {

		final Account account = new Account();
		account.setName("test");

		when(accountService.findByName(account.getName())).thenReturn(account);

		mockMvc.perform(get("/" + account.getName()))
				.andExpect(jsonPath("$.data.name").value(account.getName()))
				.andExpect(status().isOk());
	}

	@Test
	public void shouldGetCurrentAccount() throws Exception {

		final Account account = new Account();
		account.setName("test");

		when(accountService.findByName(account.getName())).thenReturn(account);

		mockMvc.perform(get("/current").principal(new UserPrincipal(account.getName())))
				.andExpect(jsonPath("$.data.name").value(account.getName()))
				.andExpect(status().isOk());
	}

	@Test
	public void shouldSaveCurrentAccount() throws Exception {

		final Account account = getStubAccount();
		String json = mapper.writeValueAsString(account);

		mockMvc.perform(put("/current").principal(new UserPrincipal(account.getName()))
						.contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isOk());

		verify(accountService, times(1)).saveChanges(eq("test"), any(Account.class));
	}

	@Test
	public void shouldCreateNewAccount() throws Exception {

		final Account account = new Account();
		account.setName("test");

		when(accountService.create(any(com.duck.saver.account.domain.User.class))).thenReturn(account);

		com.duck.saver.account.domain.User user = new com.duck.saver.account.domain.User();
		user.setUsername("test");
		user.setPassword("password");

		String json = mapper.writeValueAsString(user);

		mockMvc.perform(post("/").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(jsonPath("$.data.name").value(account.getName()))
				.andExpect(status().isOk());
	}

	private Account getStubAccount() {

		Saving saving = new Saving();
		saving.setAmount(new BigDecimal(1500));
		saving.setCurrency(Currency.USD);
		saving.setInterest(new BigDecimal("3.32"));
		saving.setDeposit(true);
		saving.setCapitalization(false);

		Item grocery = new Item();
		grocery.setTitle("Grocery");
		grocery.setAmount(new BigDecimal(10));
		grocery.setCurrency(Currency.USD);
		grocery.setPeriod(TimePeriod.DAY);
		grocery.setIcon("meal");

		Item salary = new Item();
		salary.setTitle("Salary");
		salary.setAmount(new BigDecimal(9100));
		salary.setCurrency(Currency.USD);
		salary.setPeriod(TimePeriod.MONTH);
		salary.setIcon("wallet");

		Account account = new Account();
		account.setName("test");
		account.setNote("test note");
		account.setLastSeen(new Date());
		account.setSaving(saving);
		account.setExpenses(java.util.List.of(grocery));
		account.setIncomes(java.util.List.of(salary));

		return account;
	}
}
