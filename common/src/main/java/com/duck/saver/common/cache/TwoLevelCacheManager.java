package com.duck.saver.common.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * 两级 CacheManager：每个 cacheName 由工厂函数组装 L1（Caffeine）+ L2（Redis）。
 * L1/L2 的 TTL 策略由各服务注入的工厂函数决定（AGENTS.md：账户 5min · 统计 1min · AI 分类 24h · 汇率 1h）。
 */
public class TwoLevelCacheManager implements CacheManager {

	private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();
	private final Collection<String> cacheNames;
	private final Function<String, Cache> factory;

	public TwoLevelCacheManager(Collection<String> cacheNames, Function<String, Cache> factory) {
		this.cacheNames = cacheNames;
		this.factory = factory;
	}

	@Override
	public Cache getCache(String name) {
		if (!cacheNames.contains(name)) {
			return null;
		}
		return caches.computeIfAbsent(name, factory);
	}

	@Override
	public Collection<String> getCacheNames() {
		return Collections.unmodifiableCollection(cacheNames);
	}
}
