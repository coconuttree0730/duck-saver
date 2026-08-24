package com.duck.saver.auth.oauth;

/**
 * 可插拔 OAuth2 Provider 抽象：每个第三方一个实现 bean，按 provider 名路由。
 * 新增第三方 = 实现本接口 + 一份 Nacos 配置，不触碰登录主流程。
 */
public interface OAuth2Provider {

	String provider();

	boolean enabled();

	/**
	 * 用授权码换取第三方档案（access_token 用完即弃，不持久化）。
	 * 凭证无效或第三方接口失败时抛出异常，由调用方转 4002。
	 */
	OAuth2Profile exchange(String code);
}
