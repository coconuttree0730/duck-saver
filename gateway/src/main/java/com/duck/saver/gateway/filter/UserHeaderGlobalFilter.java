package com.duck.saver.gateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 将已登录用户名以 X-User-Name 头透传给下游服务，
 * 下游服务的 HeaderPrincipalFilter 将其还原为 Principal。
 */
@Component
public class UserHeaderGlobalFilter implements GlobalFilter, Ordered {

	public static final String HEADER = "X-User-Name";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		Object loginIdObj = StpUtil.getLoginIdDefaultNull();
		String loginId = loginIdObj == null ? null : loginIdObj.toString();
		if (loginId != null) {
			ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
					.headers(headers -> headers.set(HEADER, loginId))
					.build();
			return chain.filter(exchange.mutate().request(mutatedRequest).build());
		}
		return chain.filter(exchange);
	}

	@Override
	public int getOrder() {
		return -100;
	}
}
