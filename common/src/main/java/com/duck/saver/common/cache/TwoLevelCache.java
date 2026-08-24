package com.duck.saver.common.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.util.concurrent.Callable;

/**
 * 两级缓存条目：L1（Caffeine，JVM 内）未命中回源 L2（Redis）并回填；
 * 写/删双层同动。值统一以 JSON 序列化进 L2。
 */
public class TwoLevelCache implements Cache {

	private final String name;
	private final Cache l1;
	private final Cache l2;

	public TwoLevelCache(String name, Cache l1, Cache l2) {
		this.name = name;
		this.l1 = l1;
		this.l2 = l2;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object getNativeCache() {
		return this;
	}

	@Override
	public ValueWrapper get(Object key) {
		ValueWrapper l1Hit = l1.get(key);
		if (l1Hit != null && l1Hit.get() != null) {
			return l1Hit;
		}
		ValueWrapper l2Hit = l2.get(key);
		if (l2Hit != null && l2Hit.get() != null) {
			Object value = l2Hit.get();
			l1.put(key, value);
			return new SimpleValueWrapper(value);
		}
		return null;
	}

	@Override
	public <T> T get(Object key, Class<T> type) {
		T value = l1.get(key, type);
		if (value != null) {
			return value;
		}
		value = l2.get(key, type);
		if (value != null) {
			l1.put(key, value);
		}
		return value;
	}

	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		ValueWrapper hit = get(key);
		if (hit != null) {
			@SuppressWarnings("unchecked")
			T value = (T) hit.get();
			return value;
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
		l2.put(key, value);
		l1.put(key, value);
	}

	@Override
	public void evict(Object key) {
		l2.evict(key);
		l1.evict(key);
	}

	@Override
	public void clear() {
		l2.clear();
		l1.clear();
	}
}
