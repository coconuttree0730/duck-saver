package com.duck.saver.gateway.config;

import cn.dev33.satoken.jwt.StpLogicJwtForMixin;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * sa-token-jwt Mixin 模式（ADR 0005）：
 * token 为可离线验签的 JWT，会话仍存 Redis——签名校验 + 吊销能力双重保障。
 */
@Configuration
public class SaTokenJwtConfig {

	@Bean
	public StpLogic getStpLogicJwtForMixin() {
		return new StpLogicJwtForMixin();
	}
}
