package com.duck.saver.notification.client;

import com.duck.saver.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AccountServiceClientFallback implements AccountServiceClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(AccountServiceClientFallback.class);

	@Override
	public ApiResponse<String> getAccount(String accountName) {
		LOGGER.error("Error during fetch account backup for account: {}", accountName);
		return ApiResponse.fail(503, "account-service unavailable");
	}
}
