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
import java.util.List;

import java.time.temporal.ChronoUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gateway 黑盒冒烟套件（唯一新增 seam，见 issue #1 Testing Decisions）。
 * 前置：docker compose up -d 已拉起全栈。只断言外部可见行为：
 * Nacos 注册、五条路由可达、Swagger 可访问、登录态门禁、健康与指标端点。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GatewaySmokeIT {

	static final String GATEWAY = "http://localhost:4000";
	static final String NACOS = "http://localhost:8848";
	static final String SMOKE_USER = "smoke-user-" + System.currentTimeMillis();
	static final String SMOKE_PASSWORD = "smoke-password";

	static Process composeProcess;
	static volatile String sharedToken;
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
		if (composeProcess.waitFor(15, java.util.concurrent.TimeUnit.MINUTES) == false) {
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

	@Test
	@Order(1)
	void allServicesRegisteredInNacos() throws Exception {
		List<String> services = List.of("gateway", "auth-service", "account-service",
				"statistics-service", "notification-service", "ai-service");

		awaitAtMost(3, ChronoUnit.valueOf("MINUTES"), () -> services.stream().allMatch(GatewaySmokeIT::registeredInNacos),
				"services registered in nacos: " + services);

		for (String service : services) {
			assertTrue(registeredInNacos(service), service + " should be registered in nacos");
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
	@Order(2)
	void allFiveRoutesReachableThroughGateway() {
		List<String> contexts = List.of("uaa", "accounts", "statistics", "notifications", "ai");

		awaitAtMost(3, ChronoUnit.valueOf("MINUTES"), () -> contexts.stream().allMatch(GatewaySmokeIT::swaggerUp),
				"swagger reachable via gateway for: " + contexts);

		for (String context : contexts) {
			assertTrue(swaggerUp(context), context + "/swagger-ui.html should be reachable through gateway");
		}
	}

	private static boolean swaggerUp(String context) {
		try {
			int status = get(GATEWAY + "/" + context + "/swagger-ui.html").statusCode();
			return status == 200;
		} catch (Exception e) {
			return false;
		}
	}

	@Test
	@Order(3)
	public void protectedRouteRejectsAnonymousRequest() throws Exception {
		awaitAtMost(2, ChronoUnit.valueOf("MINUTES"), () -> {
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

	@Test
	@Order(4)
	void registerLoginAndAccessProtectedResource() throws Exception {

		awaitAtMost(2, ChronoUnit.valueOf("MINUTES"), () -> {
					try {
						return post(GATEWAY + "/uaa/users",
								mapper.writeValueAsString(java.util.Map.of("username", SMOKE_USER, "password", SMOKE_PASSWORD))
						).statusCode() == 200;
					} catch (Exception e) {
						return false;
					}
				},
				"user registration through gateway succeeds");

		HttpResponse<String> loginResponse = post(GATEWAY + "/uaa/login",
				mapper.writeValueAsString(java.util.Map.of("username", SMOKE_USER, "password", SMOKE_PASSWORD)));
		assertEquals(200, loginResponse.statusCode(), "login should succeed: " + loginResponse.body());

		JsonNode loginBody = mapper.readTree(loginResponse.body());
		String token = loginBody.at("/data/tokenValue").asText();
		assertNotNull(token, "login response should carry token under data.tokenValue: " + loginResponse.body());
		sharedToken = token;

		HttpResponse<String> accountResponse = client.send(
				HttpRequest.newBuilder(URI.create(GATEWAY + "/accounts/current"))
						.header("satoken", token)
						.GET().build(),
				HttpResponse.BodyHandlers.ofString());
		assertEquals(200, accountResponse.statusCode(), "protected resource accessible with token");

		JsonNode accountBody = mapper.readTree(accountResponse.body());
		assertEquals(0, accountBody.at("/code").asInt(), "wrapped response code 0 on success");
	}

	@Test
	@Order(5)
	void aiServicePingThroughGatewayWithToken() throws Exception {
		assertNotNull(sharedToken, "login at order(4) must have provided a token");

		awaitAtMost(2, ChronoUnit.valueOf("MINUTES"), () -> {
					try {
						return authorizedGet(GATEWAY + "/ai/ping").statusCode() == 200;
					} catch (Exception e) {
						return false;
					}
				},
				"/ai/ping reachable with token");

		JsonNode body = mapper.readTree(authorizedGet(GATEWAY + "/ai/ping").body());
		assertEquals("pong", body.at("/data/status").asText());
	}

	private HttpResponse<String> authorizedGet(String url) throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create(url))
						.header("satoken", sharedToken)
						.GET().build(),
				HttpResponse.BodyHandlers.ofString());
	}

	@Test
	@Order(6)
	void healthAndPrometheusEndpointsExposedPerService() throws Exception {
		record Probe(String url) {
		}
		List<Probe> probes = List.of(
				new Probe("http://localhost:5000/uaa/actuator/health"),
				new Probe("http://localhost:6000/accounts/actuator/health"),
				new Probe("http://localhost:7000/statistics/actuator/prometheus"),
				new Probe("http://localhost:8000/notifications/actuator/prometheus"),
				new Probe("http://localhost:19000/ai/actuator/health"));

		for (Probe probe : probes) {
			awaitAtMost(2, ChronoUnit.valueOf("MINUTES"), () -> {
						try {
							return get(probe.url()).statusCode() == 200;
						} catch (Exception e) {
							return false;
						}
					},
					probe.url() + " responds 200");
		}
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

	private static void awaitAtMost(int amount, ChronoUnit unit, java.util.function.BooleanSupplier condition,
			String description) {
		java.time.Duration timeout = Duration.of(amount, unit);
		java.time.Instant deadline = java.time.Instant.now().plus(timeout);
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
}
