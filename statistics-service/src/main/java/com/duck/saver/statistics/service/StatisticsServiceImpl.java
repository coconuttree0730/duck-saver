package com.duck.saver.statistics.service;

import com.duck.saver.statistics.domain.Account;
import com.duck.saver.statistics.dto.CashflowEntry;
import com.duck.saver.statistics.dto.MetricResponse;
import com.duck.saver.statistics.dto.StatisticsResponse;
import com.duck.saver.statistics.entity.DataPointEntity;
import com.duck.saver.statistics.mapper.DataPointMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class StatisticsServiceImpl implements StatisticsService {

	private static final Logger log = LoggerFactory.getLogger(StatisticsServiceImpl.class);

	@Autowired
	private DataPointMapper dataPointMapper;

	@Override
	@Cacheable(cacheNames = "statistics", key = "#accountName")
	public StatisticsResponse findByAccountName(String accountName) {

		List<DataPointEntity> points = dataPointMapper.selectList(
				new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DataPointEntity>()
						.eq(DataPointEntity::getAccountName, accountName)
						.orderByAsc(DataPointEntity::getDate));

		StatisticsResponse response = new StatisticsResponse();
		response.setAccount(accountName);
		response.setCashflow(points.stream()
				.map(p -> {
					BigDecimal[] amounts = amountsOf(p);
					return new CashflowEntry(p.getDate(), amounts[1], amounts[0], amounts[2]);
				})
				.toList());

		if (points.isEmpty()) {
			response.setExpense(new MetricResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
			response.setIncome(new MetricResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
			response.setSaving(new MetricResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
			return response;
		}

		DataPointEntity latest = points.get(points.size() - 1);
		DataPointEntity previous = points.size() > 1 ? points.get(points.size() - 2) : null;

		BigDecimal[] latestAmounts = amountsOf(latest); // [expense, income, saving]
		BigDecimal[] previousAmounts = previous != null ? amountsOf(previous)
				: new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};

		response.setExpense(metric(latestAmounts[0], previousAmounts[0]));
		response.setIncome(metric(latestAmounts[1], previousAmounts[1]));
		response.setSaving(metric(latestAmounts[2], previousAmounts[2]));
		return response;
	}

	@Override
	@CacheEvict(cacheNames = "statistics", key = "#accountName")
	public void save(String accountName, Account account) {

		LocalDate today = LocalDate.now();

		BigDecimal incomesTotal = total(account.getIncomes());
		BigDecimal expensesTotal = total(account.getExpenses());
		BigDecimal savingAmount = account.getSaving() != null && account.getSaving().getAmount() != null
				? account.getSaving().getAmount()
				: BigDecimal.ZERO;

		String statisticsJson = """
				{"INCOMES_AMOUNT": %s, "EXPENSES_AMOUNT": %s, "SAVING_AMOUNT": %s}"""
				.formatted(incomesTotal, expensesTotal, savingAmount);

		DataPointEntity existing = dataPointMapper.selectOne(
				new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DataPointEntity>()
						.eq(DataPointEntity::getAccountName, accountName)
						.eq(DataPointEntity::getDate, today));

		DataPointEntity point = existing != null ? existing : new DataPointEntity();
		point.setAccountName(accountName);
		point.setDate(today);
		point.setIncomes(itemsJson(account.getIncomes()));
		point.setExpenses(itemsJson(account.getExpenses()));
		point.setStatistics(statisticsJson);

		if (existing != null) {
			dataPointMapper.updateById(point);
		} else {
			dataPointMapper.insert(point);
		}

		log.debug("datapoint saved for {} at {}", accountName, today);
	}

	private BigDecimal[] amountsOf(DataPointEntity point) {
		// statistics JSON: {INCOMES_AMOUNT: x, EXPENSES_AMOUNT: y, SAVING_AMOUNT: z}
		String json = point.getStatistics() == null ? "{}" : point.getStatistics();
		return new BigDecimal[]{
				extract(json, "EXPENSES_AMOUNT"),
				extract(json, "INCOMES_AMOUNT"),
				extract(json, "SAVING_AMOUNT")
		};
	}

	private BigDecimal extract(String json, String key) {
		int idx = json.indexOf(key);
		if (idx < 0) {
			return BigDecimal.ZERO;
		}
		int colon = json.indexOf(':', idx);
		int comma = json.indexOf(',', colon);
		int end = comma < 0 ? json.indexOf('}', colon) : comma;
		try {
			return new BigDecimal(json.substring(colon + 1, end).trim());
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}

	private String itemsJson(List<Account.Item> items) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < items.size(); i++) {
			Account.Item item = items.get(i);
			if (i > 0) {
				sb.append(", ");
			}
			sb.append("{\"title\": \"").append(item.getTitle()).append("\", \"amount\": ").append(item.getAmount())
					.append("}");
		}
		return sb.append("]").toString();
	}

	private BigDecimal total(List<Account.Item> items) {
		return items == null ? BigDecimal.ZERO
				: items.stream().map(Account.Item::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private MetricResponse metric(BigDecimal current, BigDecimal previous) {
		BigDecimal percentChange = BigDecimal.ZERO.compareTo(previous) == 0 ? BigDecimal.ZERO
				: current.subtract(previous)
						.divide(previous.abs(), 4, RoundingMode.HALF_UP)
						.multiply(BigDecimal.valueOf(100))
						.setScale(1, RoundingMode.HALF_UP);
		return new MetricResponse(current, previous, percentChange);
	}
}
