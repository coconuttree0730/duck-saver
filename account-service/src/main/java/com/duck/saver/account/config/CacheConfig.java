package com.duck.saver.account.config;

import com.duck.saver.common.cache.TwoLevelCache;
import com.duck.saver.common.cache.TwoLevelCacheManager;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 多级缓存（AGENTS.md 定稿）：L1 Caffeine（JVM，短 TTL 限跨节点不一致窗口）
 * + L2 Redis（账户 5min · 统计 1min）。AI 分类 24h / 汇率 1h 的 L1 长效位
 * 为阶段三预留。值统一 JSON 序列化。
 */
@Configuration
@EnableCaching
public class CacheConfig {

	public static final String CACHE_ACCOUNTS = "accounts";
	public static final String CACHE_STATISTICS = "statistics";
	private static final List<String> CACHE_NAMES =
			List.of(CACHE_ACCOUNTS, CACHE_STATISTICS, "aiClassify", "exchangeRate");

	@Bean
	public CacheManager cacheManager(org.springframework.data.redis.connection.RedisConnectionFactory cf) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
				ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
		GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

		Map<String, Duration> l2Ttl = Map.of(
				CACHE_ACCOUNTS, Duration.ofMinutes(5),
				CACHE_STATISTICS, Duration.ofMinutes(1));
		Map<String, Duration> l1Ttl = Map.of(
				CACHE_ACCOUNTS, Duration.ofSeconds(60),
				CACHE_STATISTICS, Duration.ofSeconds(30),
				"aiClassify", Duration.ofHours(24),
				"exchangeRate", Duration.ofHours(1));

		org.springframework.data.redis.core.RedisTemplate<String, Object> template =
				jsonRedisTemplate(cf, serializer);

		return new TwoLevelCacheManager(CACHE_NAMES, name -> new TwoLevelCache(name,
				new org.springframework.cache.caffeine.CaffeineCache(name, Caffeine.newBuilder()
						.expireAfterWrite(l1Ttl.getOrDefault(name, Duration.ofSeconds(60)))
						.maximumSize(1000)
						.build()),
				new com.duck.saver.common.cache.RedisJsonCache(name, template,
						l2Ttl.getOrDefault(name, Duration.ofMinutes(5)))));
	}

	@Bean
	public org.springframework.data.redis.core.RedisTemplate<String, Object> jsonRedisTemplate(
			org.springframework.data.redis.connection.RedisConnectionFactory cf,
			GenericJackson2JsonRedisSerializer serializer) {
		org.springframework.data.redis.core.RedisTemplate<String, Object> template =
				new org.springframework.data.redis.core.RedisTemplate<>();
		template.setConnectionFactory(cf);
		template.setKeySerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
		template.setHashKeySerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
		template.setValueSerializer(serializer);
		template.afterPropertiesSet();
		return template;
	}
}
