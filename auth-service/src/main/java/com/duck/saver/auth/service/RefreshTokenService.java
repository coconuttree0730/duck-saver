package com.duck.saver.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * refresh_token 自实现存储（sa-token 1.44 无原生支持）：
 * Redis 键 duck:auth:refresh:{token} → username，TTL 30 天，使用后轮换作废。
 */
@Service
public class RefreshTokenService {

	private static final String KEY_PREFIX = "duck:auth:refresh:";
	private static final Duration TTL = Duration.ofDays(30);

	@Autowired
	private StringRedisTemplate redisTemplate;

	public String issue(String username) {
		String token = UUID.randomUUID().toString();
		redisTemplate.opsForValue().set(KEY_PREFIX + token, username, TTL);
		return token;
	}

	/**
	 * 校验并轮换：旧 refresh 立即作废，返回其用户名。
	 */
	public String rotate(String refreshToken) {
		String key = KEY_PREFIX + refreshToken;
		String username = redisTemplate.opsForValue().get(key);
		if (username == null) {
			throw new IllegalArgumentException("invalid refresh token");
		}
		redisTemplate.delete(key);
		return username;
	}

	public void revoke(String refreshToken) {
		redisTemplate.delete(KEY_PREFIX + refreshToken);
	}
}
