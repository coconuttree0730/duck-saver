package com.duck.saver.common;

import com.duck.saver.common.web.ResultWrapAdvice;
import com.duck.saver.common.web.GlobalExceptionHandler;
import com.duck.saver.common.web.HeaderPrincipalFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonWebAutoConfiguration {

	@Bean
	public GlobalExceptionHandler globalExceptionHandler() {
		return new GlobalExceptionHandler();
	}

	@Bean
	public ResultWrapAdvice resultWrapAdvice(ObjectMapper objectMapper) {
		return new ResultWrapAdvice(objectMapper);
	}

	@Bean
	public FilterRegistrationBean<HeaderPrincipalFilter> headerPrincipalFilter() {
		FilterRegistrationBean<HeaderPrincipalFilter> registration = new FilterRegistrationBean<>(new HeaderPrincipalFilter());
		registration.addUrlPatterns("/*");
		registration.setOrder(Integer.MIN_VALUE);
		return registration;
	}
}
