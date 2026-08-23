package com.duck.saver.account.client;

import com.duck.saver.account.client.dto.StatisticsPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author cdov
 */
@Component
public class StatisticsServiceClientFallback implements StatisticsServiceClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsServiceClientFallback.class);
    @Override
    public void updateStatistics(String accountName, StatisticsPayload payload) {
        LOGGER.error("Error during update statistics for account: {}", accountName);
    }
}
