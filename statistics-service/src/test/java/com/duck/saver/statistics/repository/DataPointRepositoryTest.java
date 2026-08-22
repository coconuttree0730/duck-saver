package com.duck.saver.statistics.repository;

import com.duck.saver.statistics.domain.Currency;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import com.duck.saver.statistics.domain.timeseries.DataPoint;
import com.duck.saver.statistics.domain.timeseries.DataPointId;
import com.duck.saver.statistics.domain.timeseries.ItemMetric;
import com.duck.saver.statistics.domain.timeseries.StatisticMetric;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataMongoTest
@Testcontainers
class DataPointRepositoryTest {

	@Container
	@ServiceConnection
	static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

	@Autowired
	private DataPointRepository repository;

	@Test
	public void shouldSaveDataPoint() {

		ItemMetric salary = new ItemMetric("salary", new BigDecimal(20_000));

		ItemMetric grocery = new ItemMetric("grocery", new BigDecimal(1_000));
		ItemMetric vacation = new ItemMetric("vacation", new BigDecimal(2_000));

		DataPointId pointId = new DataPointId("test-account", new Date(0));

		DataPoint point = new DataPoint();
		point.setId(pointId);
		point.setIncomes(new HashSet<>(Set.of(salary)));
		point.setExpenses(new HashSet<>(Set.of(grocery, vacation)));
		point.setStatistics(Map.of(
				StatisticMetric.SAVING_AMOUNT, new BigDecimal(400_000),
				StatisticMetric.INCOMES_AMOUNT, new BigDecimal(20_000),
				StatisticMetric.EXPENSES_AMOUNT, new BigDecimal(3_000)
		));
		point.setRates(Map.of(Currency.USD, BigDecimal.ONE));

		repository.save(point);

		List<DataPoint> points = repository.findByIdAccount(pointId.getAccount());
		assertEquals(1, points.size());
		assertEquals(pointId.getDate(), points.get(0).getId().getDate());
		assertEquals(point.getStatistics().size(), points.get(0).getStatistics().size());
		assertEquals(point.getIncomes().size(), points.get(0).getIncomes().size());
		assertEquals(point.getExpenses().size(), points.get(0).getExpenses().size());
	}

	@Test
	public void shouldRewriteDataPointWithinADay() {

		final BigDecimal earlyAmount = new BigDecimal(100);
		final BigDecimal lateAmount = new BigDecimal(200);

		DataPointId pointId = new DataPointId("test-account-2", new Date(0));

		DataPoint earlier = new DataPoint();
		earlier.setId(pointId);
		earlier.setIncomes(new HashSet<>());
		earlier.setExpenses(new HashSet<>());
		earlier.setStatistics(Map.of(
				StatisticMetric.SAVING_AMOUNT, earlyAmount
		));

		repository.save(earlier);

		DataPoint later = new DataPoint();
		later.setId(pointId);
		later.setIncomes(new HashSet<>());
		later.setExpenses(new HashSet<>());
		later.setStatistics(Map.of(
				StatisticMetric.SAVING_AMOUNT, lateAmount
		));

		repository.save(later);

		List<DataPoint> points = repository.findByIdAccount(pointId.getAccount());
		assertEquals(1, points.size());
		assertEquals(lateAmount, points.get(0).getStatistics().get(StatisticMetric.SAVING_AMOUNT));
	}
}
