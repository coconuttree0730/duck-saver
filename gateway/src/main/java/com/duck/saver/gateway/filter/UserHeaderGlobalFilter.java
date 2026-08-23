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
 *
 * 注意：这里显式读取 satoken 头并按 token 反查登录 id
 * （sa-token 1.42+ 的上下文在 GlobalFilter 阶段不可用）。
 */
@Component
public class UserHeaderGlobalFilter implements GlobalFilter, Ordered {

	public static final String HEADER = "X-User-Name";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		String token = exchange.getRequest().getHeaders().getFirst("satoken");

		if (token != null && !token.isBlank()) {
			try {
				Object loginId = StpUtil.getLoginIdByToken(token);
				if (loginId != null) {
					ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
							.headers(headers -> headers.set(HEADER, loginId.toString()))
							.build();
					return chain.filter(exchange.mutate().request(mutatedRequest).build());
				}
			} catch (Exception ignored) {
				// token 无效时不透传身份，交由下游/网关鉴权逻辑处理
			}
		}
		return chain.filter(exchange);
	}

	@Override
	public int getOrder() {
		return -100;
	}
}
