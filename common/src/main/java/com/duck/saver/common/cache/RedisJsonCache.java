package com.duck.saver.common.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * L2 缓存的 Redis 实现：值经 JSON 序列化（模板侧配置），键为 {@code <cacheName>::<key>}，按名定 TTL。
 */
public class RedisJsonCache implements Cache {

	private final String name;
	private final RedisTemplate<String, Object> template;
	private final Duration ttl;

	public RedisJsonCache(String name, RedisTemplate<String, Object> template, Duration ttl) {
		this.name = name;
		this.template = template;
		this.ttl = ttl;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object getNativeCache() {
		return template;
	}

	private String redisKey(Object key) {
		return name + "::" + key;
	}

	@Override
	public ValueWrapper get(Object key) {
		Object value = template.opsForValue().get(redisKey(key));
		return value == null ? null : new SimpleValueWrapper(value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Object key, Class<T> type) {
		Object value = template.opsForValue().get(redisKey(key));
		return value == null ? null : (T) value;
	}

	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		ValueWrapper hit = get(key);
		if (hit != null) {
			return (T) hit.get();
		}
		try {
			T value = valueLoader.call();
			put(key, value);
			return value;
		} catch (Exception e) {
			throw new ValueRetrievalException(key, valueLoader, e);
		}
	}

	@Override
	public void put(Object key, Object value) {
		template.opsForValue().set(redisKey(key), value, ttl);
	}

	@Override
	public void evict(Object key) {
		template.delete(redisKey(key));
	}

	@Override
	public void clear() {
		Set<String> keys = template.keys(name + "::*");
		if (keys != null && !keys.isEmpty()) {
			template.delete(keys);
		}
	}
}
