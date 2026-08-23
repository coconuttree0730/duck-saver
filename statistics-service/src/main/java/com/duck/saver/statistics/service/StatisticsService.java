package com.duck.saver.statistics.service;

import com.duck.saver.statistics.domain.Account;
import com.duck.saver.statistics.dto.StatisticsResponse;

public interface StatisticsService {

	StatisticsResponse findByAccountName(String accountName);

	void save(String accountName, Account account);
}
