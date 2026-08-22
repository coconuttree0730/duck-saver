package com.duck.saver.statistics.controller;

import com.duck.saver.statistics.domain.Account;
import com.duck.saver.statistics.domain.timeseries.DataPoint;
import com.duck.saver.statistics.service.StatisticsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
public class StatisticsController {

	@Autowired
	private StatisticsService statisticsService;

	@GetMapping(value = "/current")
	public List<DataPoint> getCurrentAccountStatistics(Principal principal) {
		return statisticsService.findByAccountName(principal.getName());
	}

	@GetMapping(value = "/{accountName}")
	public List<DataPoint> getStatisticsByAccountName(@PathVariable String accountName) {
		return statisticsService.findByAccountName(accountName);
	}

	@PutMapping(value = "/{accountName}")
	public void saveAccountStatistics(@PathVariable String accountName, @Valid @RequestBody Account account) {
		statisticsService.save(accountName, account);
	}
}
