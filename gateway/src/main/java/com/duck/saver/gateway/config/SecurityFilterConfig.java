package com.duck.saver.gateway.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Sa-Token 网关统一鉴权（ADR 0005）：
 * 放行 /uaa/login、/uaa/oauth2/** 与开放注册（POST /uaa/users），其余业务路径要求登录态。
 * Swagger / API 文档路径放行，便于开发期查阅契约。
 * 登录用户身份由 UserHeaderGlobalFilter 以 X-User-Name 头透传给下游服务。
 */
@Configuration
public class SecurityFilterConfig {

	private static final String[] OPEN_PATHS = {
			"/uaa/login",
			"/uaa/oauth2/**",
			"/*/v3/api-docs/**",
			"/*/swagger-ui/**",
			"/*/swagger-ui.html"
	};

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Bean
	public SaReactorFilter saReactorFilter() {
		return new SaReactorFilter()
				.addInclude("/**")
				.addExclude(OPEN_PATHS)
				.setAuth(obj -> {
					if (isOpenRegistration()) {
						return;
					}
					StpUtil.checkLogin();
				})
				.setError(e -> {
					SaHolder.getResponse().setStatus(401);
					try {
						return objectMapper.writeValueAsString(
								Map.of("code", 401, "message", "not logged in"));
					} catch (Exception ex) {
						return "{\"code\":401,\"message\":\"not logged in\"}";
					}
				});
	}

	private boolean isOpenRegistration() {
		return "POST".equalsIgnoreCase(SaHolder.getRequest().getMethod())
				&& SaHolder.getRequest().getRequestPath().startsWith("/uaa/users");
	}
}
