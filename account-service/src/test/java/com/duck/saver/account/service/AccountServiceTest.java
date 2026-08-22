package com.duck.saver.account.service;

import com.duck.saver.account.client.AuthServiceClient;
import com.duck.saver.account.client.StatisticsServiceClient;
import com.duck.saver.account.domain.Account;
import com.duck.saver.account.domain.Currency;
import com.duck.saver.account.domain.Item;
import com.duck.saver.account.domain.Saving;
import com.duck.saver.account.domain.User;
import com.duck.saver.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class AccountServiceTest {

	@InjectMocks
	private AccountServiceImpl accountService;

	@Mock
	private StatisticsServiceClient statisticsClient;

	@Mock
	private AuthServiceClient authClient;

	@Mock
	private AccountRepository repository;

	@BeforeEach
	public void setup() throws Exception {
		openMocks(this).close();
	}

	@Test
	public void shouldFindByName() {

		final Account account = new Account();
		account.setName("test");

		when(repository.findByName(account.getName())).thenReturn(account);
		Account found = accountService.findByName(account.getName());

		assertEquals(account, found);
	}

	@Test
	public void shouldFailWhenNameIsEmpty() {
		assertThrows(IllegalArgumentException.class, () -> accountService.findByName(""));
	}

	@Test
	public void shouldCreateAccountWithGivenUser() {

		User user = new User();
		user.setUsername("test");

		when(repository.findByName(user.getUsername())).thenReturn(null);

		Account account = accountService.create(user);

		assertEquals(user.getUsername(), account.getName());
		assertEquals(0, account.getSaving().getAmount().intValue());
		assertEquals(Currency.getDefault(), account.getSaving().getCurrency());
		assertEquals(0, account.getSaving().getInterest().intValue());
		assertEquals(false, account.getSaving().getDeposit());
		assertEquals(false, account.getSaving().getCapitalization());
		assertNotNull(account.getLastSeen());

		verify(authClient, times(1)).createUser(user);
		verify(repository, times(1)).save(account);
	}

	@Test
	public void shouldFailToCreateAccountWhenUserAlreadyExists() {

		User user = new User();
		user.setUsername("test");

		Account existing = new Account();
		existing.setName("test");
		when(repository.findByName(user.getUsername())).thenReturn(existing);

		assertThrows(IllegalArgumentException.class, () -> accountService.create(user));
	}

	@Test
	public void shouldSaveChangesWhenUpdatedAccountGiven() {

		Item grocery = new Item();
		grocery.setTitle("Grocery");
		grocery.setAmount(new BigDecimal(10));
		grocery.setCurrency(Currency.USD);
		grocery.setPeriod(com.duck.saver.account.domain.TimePeriod.DAY);
		grocery.setIcon("meal");

		Saving saving = new Saving();
		saving.setAmount(new BigDecimal(1500));
		saving.setCurrency(Currency.USD);
		saving.setInterest(new BigDecimal("3.32"));
		saving.setDeposit(true);
		saving.setCapitalization(false);

		Account update = new Account();
		update.setIncomes(java.util.List.of());
		update.setExpenses(java.util.List.of(grocery));
		update.setSaving(saving);

		Account account = new Account();
		account.setName("test");
		account.setLastSeen(new Date());

		when(repository.findByName("test")).thenReturn(account);

		accountService.saveChanges("test", update);

		assertEquals(update.getNote(), account.getNote());
		assertEquals(update.getIncomes(), account.getIncomes());
		assertEquals(update.getExpenses(), account.getExpenses());
		assertEquals(update.getSaving(), account.getSaving());
		assertNotNull(account.getLastSeen());

		verify(statisticsClient, times(1)).updateStatistics(account.getName(), account);
	}
}
