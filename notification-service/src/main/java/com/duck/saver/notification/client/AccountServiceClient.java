package com.duck.saver.notification.client;

import com.duck.saver.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "account-service", fallback = AccountServiceClientFallback.class)
public interface AccountServiceClient {

	@GetMapping(value = "/accounts/{accountName}")
	ApiResponse<String> getAccount(@PathVariable("accountName") String accountName);

}
