package com.duck.saver.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gateway 黑盒冒烟套件（唯一新增 seam）。
 * 覆盖：Nacos 注册、五条路由、Swagger、登录门禁、JWT 登录/刷新轮换、
 * 账户与交易记录契约流、统计聚合、通知设置、AI 探活、健康与指标端点。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GatewaySmokeIT {

	static final String GATEWAY = "http://localhost:4000";
	static final String NACOS = "http://localhost:8848";
	static final String SMOKE_USER = "smoke-user-" + System.currentTimeMillis();
	static final String SMOKE_PASSWORD = "smoke-password";

	static Process composeProcess;
	static volatile String sharedToken;
	static volatile String sharedRefreshToken;

	static final HttpClient client = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(Duration.ofSeconds(10))
			.build();
	static final ObjectMapper mapper = new ObjectMapper();

	@BeforeAll
	static void startStack() throws Exception {
		System.out.println(">>> docker compose up -d --build");
		composeProcess = new ProcessBuilder("docker", "compose", "up", "-d", "--build")
				.directory(new java.io.File(".."))
				.inheritIO()
				.start();
		if (!composeProcess.waitFor(15, java.util.concurrent.TimeUnit.MINUTES)) {
			throw new IllegalStateException("docker compose up timed out");
		}
		assertEquals(0, composeProcess.exitValue(), "docker compose up failed");
	}

	@AfterAll
	static void stopStack() throws Exception {
		if (composeProcess != null) {
			new ProcessBuilder("docker", "compose", "down", "-v", "--remove-orphans")
					.directory(new java.io.File(".."))
					.inheritIO()
					.start()
					.waitFor(5, java.util.concurrent.TimeUnit.MINUTES);
		}
	}

	private static boolean registeredInNacos(String serviceName) {
		try {
			HttpResponse<String> response = client.send(
					HttpRequest.newBuilder(URI.create(NACOS + "/nacos/v1/ns/instance/list?serviceName=" + serviceName))
							.GET().build(),
					HttpResponse.BodyHandlers.ofString());
			JsonNode body = mapper.readTree(response.body());
			return body.path("hosts").isArray() && body.path("hosts").size() > 0;
		} catch (Exception e) {
			return false;
		}
	}

	@Test
	@Order(1)
	void allServicesRegisteredInNacos() {

		List<String> services = List.of("gateway", "auth-service", "account-service",
				"statistics-service", "notification-service", "ai-service");

		awaitAtMost(3, ChronoUnit.MINUTES, () -> services.stream().allMatch(GatewaySmokeIT::registeredInNacos),
				"services registered in nacos: " + services);

		for (String service : services) {
			assertTrue(registeredInNacos(service), service + " should be registered in nacos");
		}
	}

	private static boolean swaggerUp(String context) {
		try {
			return get(GATEWAY + "/" + context + "/swagger-ui.html").statusCode() == 200;
		} catch (Exception e) {
			return false;
		}
	}

	@Test
	@Order(2)
	void allFiveRoutesReachableThroughGateway() {

		List<String> contexts = List.of("uaa", "accounts", "statistics", "notifications", "ai");

		awaitAtMost(3, ChronoUnit.MINUTES, () -> contexts.stream().allMatch(GatewaySmokeIT::swaggerUp),
				"swagger reachable via gateway for: " + contexts);

		for (String context : contexts) {
			assertTrue(swaggerUp(context), context + "/swagger-ui.html should be reachable through gateway");
		}
	}

	@Test
	@Order(3)
	public void protectedRouteRejectsAnonymousRequest() throws Exception {

		awaitAtMost(2, ChronoUnit.MINUTES, () -> {
					try {
						return get(GATEWAY + "/accounts/current").statusCode() == 401;
					} catch (Exception e) {
						return false;
					}
				},
				"/accounts/current returns 401 without token");

		HttpResponse<String> response = get(GATEWAY + "/accounts/current");
		assertEquals(401, response.statusCode());
		assertTrue(response.body().contains("not logged in"), "401 body should explain login requirement");
	}

	private static HttpResponse<String> login() throws Exception {
		return post(GATEWAY + "/uaa/login",
				mapper.writeValueAsString(Map.of("username", SMOKE_USER, "password", SMOKE_PASSWORD)));
	}

	@Test
	@Order(4)
	void registerLoginAndAccessProtectedResource() throws Exception {

		awaitAtMost(2, ChronoUnit.MINUTES, () -> {
					try {
						return post(GATEWAY + "/uaa/users",
								mapper.writeValueAsString(Map.of("username", SMOKE_USER, "password", SMOKE_PASSWORD))
						).statusCode() == 200;
					} catch (Exception e) {
						return false;
					}
				},
				"user registration through gateway succeeds");

		JsonNode loginBody = read(login());
		assertEquals(200, loginBody.at("/code").asInt(), "login should succeed");

		String token = loginBody.at("/data/tokenValue").asText();
		assertNotNull(token, "login response should carry JWT under data.tokenValue");
		assertNotNull(loginBody.at("/data/refresh_token").asText(), "login should carry refresh_token");
		assertTrue(loginBody.at("/data/expires_in").asLong() > 0, "expires_in should be positive");
		assertEquals("satoken", loginBody.at("/data/tokenName").asText());

		sharedToken = token;
		sharedRefreshToken = loginBody.at("/data/refresh_token").asText();

		// 注册后创建同名账户，使 /accounts/current 有数据（契约流）
		assertEquals(200, authorizedPost(GATEWAY + "/accounts",
				mapper.writeValueAsString(Map.of("name", SMOKE_USER, "currency", "CNY"))).statusCode(),
				"account creation for fresh user should succeed");

		JsonNode current = read(authorizedGet(GATEWAY + "/accounts/current"));
		assertEquals(200, current.at("/code").asInt(), "protected resource accessible with token");
		assertEquals(SMOKE_USER, current.at("/data/name").asText());
	}

	@Test
	@Order(5)
	void refreshTokenRotationWorksAndOldRefreshIsRejected() throws Exception {

		assertNotNull(sharedRefreshToken, "order(4) must provide refresh token");

		JsonNode refreshed = read(authorizedPost(GATEWAY + "/uaa/token/refresh",
				mapper.writeValueAsString(Map.of("refresh_token", sharedRefreshToken))));
		assertEquals(200, refreshed.at("/code").asInt(), "refresh should succeed");
		String newAccess = refreshed.at("/data/tokenValue").asText();
		String newRefresh = refreshed.at("/data/refresh_token").asText();
		assertNotNull(newAccess);
		assertNotNull(newRefresh);

		HttpResponse<String> reused = authorizedPost(GATEWAY + "/uaa/token/refresh",
				mapper.writeValueAsString(Map.of("refresh_token", sharedRefreshToken)));
		assertEquals(400, reused.statusCode(), "rotated refresh token must be rejected");

		sharedToken = newAccess;
		sharedRefreshToken = newRefresh;
	}

	@Test
	@Order(6)
	void accountAndTransactionContractFlow() throws Exception {

		String accountName = "acc-" + System.currentTimeMillis();

		assertEquals(200, authorizedPost(GATEWAY + "/accounts",
				mapper.writeValueAsString(Map.of("name", accountName, "currency", "CNY"))).statusCode(),
				"account creation should succeed");

		String item = mapper.writeValueAsString(Map.of(
				"title", "午餐",
				"amount", 48.00,
				"currency", "CNY",
				"category", "餐饮",
				"type", "EXPENSE",
				"date", java.time.LocalDate.now().toString()));

		assertEquals(200, authorizedPost(GATEWAY + "/accounts/" + accountName + "/items", item).statusCode(),
				"adding transaction should succeed");

		JsonNode account = read(authorizedGet(GATEWAY + "/accounts/" + accountName));
		assertEquals(accountName, account.at("/data/name").asText());
		assertEquals(1, account.at("/data/items").size(), "transaction should be listed");
		String itemId = account.at("/data/items/0/id").asText();

		String badItem = mapper.writeValueAsString(Map.of(
				"title", "神秘消费",
				"amount", 10.00,
				"currency", "CNY",
				"category", "神秘分类",
				"type", "EXPENSE",
				"date", java.time.LocalDate.now().toString()));
		assertEquals(400, authorizedPost(GATEWAY + "/accounts/" + accountName + "/items", badItem).statusCode(),
				"invalid category must be rejected");

		assertEquals(200, authorizedDelete(GATEWAY + "/accounts/" + accountName + "/items/" + itemId).statusCode());
		assertEquals(0, read(authorizedGet(GATEWAY + "/accounts/" + accountName)).at("/data/items").size());
	}

	@Test
	@Order(7)
	void statisticsAggregateRoundTrip() throws Exception {

		String payload = """
				{ "incomes": [ { "title": "Salary", "amount": 9100 } ],
				  "expenses": [ { "title": "Grocery", "amount": 10 } ],
				  "saving": { "amount": 1500 } }
				""";

		assertEquals(200, authorizedPut(GATEWAY + "/statistics/" + SMOKE_USER, payload).statusCode(),
				"statistics save should succeed");

		JsonNode stats = read(authorizedGet(GATEWAY + "/statistics/" + SMOKE_USER));
		assertEquals(SMOKE_USER, stats.at("/data/account").asText());
		assertTrue(stats.at("/data/cashflow").isArray(), "cashflow series should be present");
		assertEquals(0, stats.at("/data/income/currentValue").decimalValue()
				.compareTo(java.math.BigDecimal.valueOf(9100)));
	}

	@Test
	@Order(8)
	void notificationSettingsRoundTrip() throws Exception {

		String payload = mapper.writeValueAsString(Map.of(
				"email", SMOKE_USER + "@example.com",
				"frequency", "WEEKLY",
				"enabled", true));

		assertEquals(200, authorizedPut(GATEWAY + "/notifications/recipients/current", payload).statusCode(),
				"saving settings should succeed");

		JsonNode settings = read(authorizedGet(GATEWAY + "/notifications/recipients/current"));
		assertEquals(SMOKE_USER, settings.at("/data/recipient/name").asText());
		assertEquals("WEEKLY", settings.at("/data/recipient/frequency").asText());
		assertTrue(settings.at("/data/notificationConfig").isArray());
	}

	@Test
	@Order(9)
	void aiServicePingThroughGatewayWithToken() throws Exception {

		assertNotNull(sharedToken, "login at order(4) must have provided a token");

		awaitAtMost(2, ChronoUnit.MINUTES, () -> {
					try {
						return authorizedGet(GATEWAY + "/ai/ping").statusCode() == 200;
					} catch (Exception e) {
						return false;
					}
				},
				"/ai/ping reachable with token");

		JsonNode body = read(authorizedGet(GATEWAY + "/ai/ping"));
		assertEquals("pong", body.at("/data/status").asText());
	}

	private static HttpResponse<String> get(String url) throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
				HttpResponse.BodyHandlers.ofString());
	}

	private static HttpResponse<String> post(String url, String json) throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create(url))
						.header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString(json)).build(),
				HttpResponse.BodyHandlers.ofString());
	}

	private static HttpResponse<String> authorizedGet(String url) throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create(url))
						.header("satoken", sharedToken)
						.GET().build(),
				HttpResponse.BodyHandlers.ofString());
	}

	private static HttpResponse<String> authorizedPut(String url, String json) throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create(url))
						.header("Content-Type", "application/json")
						.header("satoken", sharedToken)
						.PUT(HttpRequest.BodyPublishers.ofString(json)).build(),
				HttpResponse.BodyHandlers.ofString());
	}

	private static HttpResponse<String> authorizedDelete(String url) throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create(url))
						.header("satoken", sharedToken)
						.DELETE().build(),
				HttpResponse.BodyHandlers.ofString());
	}

	private static HttpResponse<String> authorizedPost(String url, String json) throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create(url))
						.header("Content-Type", "application/json")
						.header("satoken", sharedToken)
						.POST(HttpRequest.BodyPublishers.ofString(json)).build(),
				HttpResponse.BodyHandlers.ofString());
	}

	private JsonNode read(HttpResponse<String> response) throws Exception {
		return mapper.readTree(response.body());
	}

	private static void awaitAtMost(int amount, ChronoUnit unit,
			java.util.function.BooleanSupplier condition, String description) {

		java.time.Instant deadline = java.time.Instant.now().plus(Duration.of(amount, unit));
		while (java.time.Instant.now().isBefore(deadline)) {
			if (condition.getAsBoolean()) {
				return;
			}
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		assertTrue(condition.getAsBoolean(), "timed out waiting for: " + description);
	}

	@Test
	@Order(10)
	void healthAndPrometheusEndpointsExposedPerService() throws Exception {

		record Probe(String url) {
		}
		List<Probe> probes = List.of(
				new Probe("http://localhost:5000/uaa/actuator/health"),
				new Probe("http://localhost:6000/actuator/health"),
				new Probe("http://localhost:7000/statistics/actuator/prometheus"),
				new Probe("http://localhost:8000/notifications/actuator/prometheus"),
				new Probe("http://localhost:19000/ai/actuator/health"));

		for (Probe probe : probes) {
			awaitAtMost(2, ChronoUnit.MINUTES, () -> {
						try {
							return get(probe.url()).statusCode() == 200;
						} catch (Exception e) {
							return false;
						}
					},
					probe.url() + " responds 200");
		}
	}

	@Test
	@Order(11)
	void logoutInvalidatesAccessTokenImmediately() throws Exception {

		assertEquals(200, authorizedPost(GATEWAY + "/uaa/logout", "{}").statusCode(),
				"logout should succeed");

		awaitAtMost(1, ChronoUnit.MINUTES, () -> {
					try {
						return authorizedGet(GATEWAY + "/accounts/current").statusCode() == 401;
					} catch (Exception e) {
						return false;
					}
				},
				"access token rejected after logout");
	}
}
