package com.duck.saver.account.client;

import com.duck.saver.account.client.dto.StatisticsPayload;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "statistics-service", fallback = StatisticsServiceClientFallback.class)
public interface StatisticsServiceClient {

	@PutMapping(value = "/statistics/{accountName}", consumes = MediaType.APPLICATION_JSON_VALUE)
	void updateStatistics(@PathVariable("accountName") String accountName, StatisticsPayload payload);

}
