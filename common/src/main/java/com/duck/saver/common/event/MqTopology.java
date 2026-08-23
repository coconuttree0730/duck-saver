package com.duck.saver.common.event;

/**
 * MQ 拓扑常量（AGENTS.md 定稿，两侧服务共用；声明式 Bean 在各服务内定义）。
 */
public final class MqTopology {

	public static final String EXCHANGE = "account.event.exchange";
	public static final String ACCOUNT_QUEUE = "account.event.queue";
	public static final String STATISTICS_QUEUE = "statistics.event.queue";
	public static final String DLX_EXCHANGE = "account.event.dlx";
	public static final String DLQ = "account.event.dlq";

	private MqTopology() {
	}
}
