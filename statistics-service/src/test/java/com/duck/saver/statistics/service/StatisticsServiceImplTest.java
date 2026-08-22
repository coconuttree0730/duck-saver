package com.duck.saver.statistics.service;

import com.duck.saver.statistics.domain.Account;
import com.duck.saver.statistics.domain.Currency;
import com.duck.saver.statistics.domain.Item;
import com.duck.saver.statistics.domain.Saving;
import com.duck.saver.statistics.domain.TimePeriod;
import com.duck.saver.statistics.domain.timeseries.DataPoint;
import com.duck.saver.statistics.domain.timeseries.ItemMetric;
import com.duck.saver.statistics.domain.timeseries.StatisticMetric;
import com.duck.saver.statistics.repository.DataPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.mockito.MockitoAnnotations;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatisticsServiceImplTest {

	@InjectMocks
	private StatisticsServiceImpl statisticsService;

	@Mock
	private ExchangeRatesServiceImpl ratesService;

	@Mock
	private DataPointRepository repository;

	@BeforeEach
	public void setup() throws Exception {
		MockitoAnnotations.openMocks(this).close();
	}

	@Test
	public void shouldFindDataPointListByAccountName() {
		final List<DataPoint> list = List.of(new DataPoint());
		when(repository.findByIdAccount("test")).thenReturn(list);

		List<DataPoint> result = statisticsService.findByAccountName("test");
		assertEquals(list, result);
	}

	@Test
	public void shouldFailToFindDataPointWhenAccountNameIsNull() {
		assertThrows(IllegalArgumentException.class, () -> statisticsService.findByAccountName(null));
	}

	@Test
	public void shouldFailToFindDataPointWhenAccountNameIsEmpty() {
		assertThrows(IllegalArgumentException.class, () -> statisticsService.findByAccountName(""));
	}

	@Test
	public void shouldSaveDataPoint() {

		Item salary = new Item();
		salary.setTitle("Salary");
		salary.setAmount(new BigDecimal(9100));
		salary.setCurrency(Currency.USD);
		salary.setPeriod(TimePeriod.MONTH);

		Item grocery = new Item();
		grocery.setTitle("Grocery");
		grocery.setAmount(new BigDecimal(500));
		grocery.setCurrency(Currency.RUB);
		grocery.setPeriod(TimePeriod.DAY);

		Item vacation = new Item();
		vacation.setTitle("Vacation");
		vacation.setAmount(new BigDecimal(3400));
		vacation.setCurrency(Currency.EUR);
		vacation.setPeriod(TimePeriod.YEAR);

		Saving saving = new Saving();
		saving.setAmount(new BigDecimal(1000));
		saving.setCurrency(Currency.EUR);
		saving.setInterest(new BigDecimal("3.2"));
		saving.setDeposit(true);
		saving.setCapitalization(false);

		Account account = new Account();
		account.setIncomes(List.of(salary));
		account.setExpenses(List.of(grocery, vacation));
		account.setSaving(saving);

		final Map<Currency, BigDecimal> rates = Map.of(
				Currency.EUR, new BigDecimal("0.8"),
				Currency.RUB, new BigDecimal("80"),
				Currency.USD, BigDecimal.ONE
		);

		when(ratesService.convert(any(Currency.class), any(Currency.class), any(BigDecimal.class)))
				.then(i -> ((BigDecimal) i.getArgument(2))
						.divide(rates.get(i.getArgument(0)), 4, RoundingMode.HALF_UP));

		when(ratesService.getCurrentRates()).thenReturn(rates);

		when(repository.save(any(DataPoint.class))).then(returnsFirstArg());

		DataPoint dataPoint = statisticsService.save("test", account);

		final BigDecimal expectedExpensesAmount = new BigDecimal("17.8861");
		final BigDecimal expectedIncomesAmount = new BigDecimal("298.9802");
		final BigDecimal expectedSavingAmount = new BigDecimal("1250");

		final BigDecimal expectedNormalizedSalaryAmount = new BigDecimal("298.9802");
		final BigDecimal expectedNormalizedVacationAmount = new BigDecimal("11.6361");
		final BigDecimal expectedNormalizedGroceryAmount = new BigDecimal("6.25");

		assertEquals(dataPoint.getId().getAccount(), "test");
		assertEquals(dataPoint.getId().getDate(), Date.from(LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));

		assertTrue(expectedExpensesAmount.compareTo(dataPoint.getStatistics().get(StatisticMetric.EXPENSES_AMOUNT)) == 0);
		assertTrue(expectedIncomesAmount.compareTo(dataPoint.getStatistics().get(StatisticMetric.INCOMES_AMOUNT)) == 0);
		assertTrue(expectedSavingAmount.compareTo(dataPoint.getStatistics().get(StatisticMetric.SAVING_AMOUNT)) == 0);

		ItemMetric salaryItemMetric = dataPoint.getIncomes().stream()
				.filter(i -> i.getTitle().equals(salary.getTitle()))
				.findFirst().orElseThrow();

		ItemMetric vacationItemMetric = dataPoint.getExpenses().stream()
				.filter(i -> i.getTitle().equals(vacation.getTitle()))
				.findFirst().orElseThrow();

		ItemMetric groceryItemMetric = dataPoint.getExpenses().stream()
				.filter(i -> i.getTitle().equals(grocery.getTitle()))
				.findFirst().orElseThrow();

		assertTrue(expectedNormalizedSalaryAmount.compareTo(salaryItemMetric.getAmount()) == 0);
		assertTrue(expectedNormalizedVacationAmount.compareTo(vacationItemMetric.getAmount()) == 0);
		assertTrue(expectedNormalizedGroceryAmount.compareTo(groceryItemMetric.getAmount()) == 0);

		assertEquals(rates, dataPoint.getRates());

		verify(repository, times(1)).save(dataPoint);
	}
}
