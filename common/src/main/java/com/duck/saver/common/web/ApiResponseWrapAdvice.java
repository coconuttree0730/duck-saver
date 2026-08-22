package com.duck.saver.common.web;

import com.duck.saver.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ApiResponseWrapAdvice implements ResponseBodyAdvice<Object> {

	private static final String[] EXCLUDED_PACKAGES = { "org.springframework", "org.springdoc" };

	private final ObjectMapper objectMapper;

	public ApiResponseWrapAdvice(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean supports(@NonNull MethodParameter returnType, @NonNull Class converterType) {
		String packageName = returnType.getContainingClass().getPackageName();
		for (String excluded : EXCLUDED_PACKAGES) {
			if (packageName.startsWith(excluded)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Object beforeBodyWrite(Object body, @NonNull MethodParameter returnType, @NonNull MediaType selectedContentType,
			@NonNull Class selectedConverterType, @NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
		if (body instanceof ApiResponse || body instanceof byte[] || body == null) {
			return body;
		}
		ApiResponse<Object> wrapped = ApiResponse.ok(body);
		if (body instanceof String) {
			try {
				response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
				return objectMapper.writeValueAsString(wrapped);
			}
			catch (Exception e) {
				throw new IllegalStateException("failed to wrap string response", e);
			}
		}
		return wrapped;
	}
}
