package com.duck.saver.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Principal;

/**
 * Gateway 身份透传：Gateway 完成登录态校验后以 X-User-Name 头传递用户名，
 * 本 Filter 将其还原为 Principal 供 Controller 使用。
 */
public class HeaderPrincipalFilter extends OncePerRequestFilter {

	public static final String USER_HEADER = "X-User-Name";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String user = request.getHeader(USER_HEADER);
		if (user == null || user.isBlank()) {
			filterChain.doFilter(request, response);
			return;
		}
		filterChain.doFilter(new UserHeaderRequest(request, user), response);
	}

	private static class UserHeaderRequest extends HttpServletRequestWrapper {

		private final Principal principal;

		UserHeaderRequest(HttpServletRequest request, String user) {
			super(request);
			this.principal = () -> user;
		}

		@Override
		public Principal getUserPrincipal() {
			return this.principal;
		}
	}
}
