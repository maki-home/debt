package am.ik.home;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.remoting.support.SimpleHttpServerFactoryBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import am.ik.home.debt.Debt;
import lombok.extern.slf4j.Slf4j;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.cloud.config.enabled=false",
		"api.authorization-url=http://localhost:" + DebtApplicationTests.UAA_PORT,
		"security.oauth2.resource.jwt.key-uri=http://localhost:"
				+ DebtApplicationTests.UAA_PORT + "/token_key" })
@AutoConfigureWebTestClient
@Slf4j
public class DebtApplicationTests {
	public final static int UAA_PORT = 39876;

	static SimpleHttpServerFactoryBean factoryBean;
	static String accessToken;
	@Autowired
	WebTestClient webClient;
	@LocalServerPort
	int port;

	@BeforeClass
	public static void init() throws Exception {
		TestTokenGenerator tokenGenerator = new TestTokenGenerator();
		accessToken = tokenGenerator.getAccessToken();
		factoryBean = new SimpleHttpServerFactoryBean();
		factoryBean.setPort(UAA_PORT);
		factoryBean.setContexts(Collections.singletonMap("/token_key", (exec) -> {
			String response = tokenGenerator.getTokenKey();
			exec.getResponseHeaders().add("Content-Type",
					"application/json;charset=UTF-8");
			exec.sendResponseHeaders(200, response.length());
			try (OutputStream stream = exec.getResponseBody()) {
				stream.write(response.getBytes());
			}
		}));
		factoryBean.afterPropertiesSet();
	}

	@Test
	public void getDebts() {
		webClient.get().uri("http://localhost:{port}/v1/debts", port)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).exchange()
				.expectStatus().isOk().expectBody(String.class).<String> returnResult()
				.getResponseBody().doOnNext(x -> {
					log.info("response = {}", x);
				}).subscribe();
	}

	@Test
	public void authorize() {
		webClient.get().uri("http://localhost:{port}/index.html", port).exchange()
				.expectStatus().isFound().expectHeader()
				.valueMatches(HttpHeaders.LOCATION,
						"http://localhost:" + UAA_PORT + "/oauth/authorize.+")
				.expectHeader().valueMatches(HttpHeaders.SET_COOKIE, "SESSION=.+");
	}

	@Test
	public void getDebtsFromMe() {
		webClient.get().uri("http://localhost:{port}/v1/debts?from=me", port)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).exchange()
				.expectStatus().isOk().expectBody(String.class).<String> returnResult()
				.getResponseBody().doOnNext(x -> {
					log.info("response = {}", x);
				}).subscribe();
	}

	@Test
	public void postDebts() {
		Debt debt = Debt.builder().debtDate(LocalDate.of(2017, 1, 1)).amount(100000L)
				.from("00000000-0000-0000-0000-000000000000")
				.to("00000000-0000-0000-0000-000000000001").purpose("Test").build();
		webClient.post().uri("http://localhost:{port}/v1/debts", port)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.exchange(fromObject(debt)).expectStatus().isCreated()
				.expectBody(String.class).<String> returnResult().getResponseBody()
				.doOnNext(x -> {
					log.info("response = {}", x);
				}).subscribe();
	}

	@Test
	public void unauthorized() {
		Map<String, Object> response = webClient.get()
				.uri("http://localhost:{port}/v1/debts", port).exchange().expectStatus()
				.isUnauthorized()
				.expectBody(ResolvableType.forClassWithGenerics(Map.class, String.class,
						Object.class))
				.value().<Map<String, Object>> returnResult().getResponseBody();
		assertThat(response).containsEntry("error", "unauthorized").containsEntry(
				"error_description",
				"Full authentication is required to access this resource");
	}

	@Test
	public void invalidAuthorization() {
		Map<String, Object> response = webClient.get()
				.uri("http://localhost:{port}/v1/debts", port)
				.header(HttpHeaders.AUTHORIZATION, "Basic foo").exchange().expectStatus()
				.isUnauthorized()
				.expectBody(ResolvableType.forClassWithGenerics(Map.class, String.class,
						Object.class))
				.value().<Map<String, Object>> returnResult().getResponseBody();
		assertThat(response).containsEntry("error", "unauthorized").containsEntry(
				"error_description",
				"Full authentication is required to access this resource");
	}

	@Test
	public void invalidToken() {
		String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX25hbWUiOiIwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDAiLCJpc3MiOiJtYWtpLXVhYSIsImdpdmVuX25hbWUiOiJUb3NoaWFraSIsImRpc3BsYXlfbmFtZSI6Ik1ha2kgVG9zaGlha2kiLCJhdXRob3JpdGllcyI6WyJST0xFX0FETUlOIiwiUk9MRV9VU0VSIiwiUk9MRV9BQ1RVQVRPUiJdLCJjbGllbnRfaWQiOiIwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDAiLCJhdWQiOlsib2F1dGgyLXJlc291cmNlIl0sInVzZXJfaWQiOiIwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDAiLCJzY29wZSI6WyJtZW1iZXIucmVhZCIsImluY29tZS53cml0ZSIsImFjY291bnQud3JpdGUiLCJvcGVuaWQiLCJtZW1iZXIud3JpdGUiLCJpbmNvbWUucmVhZCIsIm91dGNvbWUud3JpdGUiLCJvdXRjb21lLnJlYWQiLCJhY2NvdW50LnJlYWQiXSwiZXhwIjoxNDg4NDgxMzIwLCJmYW1pbHlfbmFtZSI6Ik1ha2kiLCJpYXQiOjE0ODg0Nzk1MjAsImp0aSI6ImMzNDRlYjg4LTYzZjMtNDUxMS05ZjY2LWYyOTlhMTNkMmU5MSIsImVtYWlsIjoibWFraUBleGFtcGxlLmNvbSJ9.hB1FcTCjMq0WTZ85_RyfFJK2V0eYypWzuZepdUVi7H3ba-HNSvXpmi916sFlnd4Ojgt3AfqIk-7TZL2x1ED5ufQcYJw053R12fkpi49NvUF3mVaxYMbq3dCoEajugpUS6ajeh4Hdsj1KG7xRciFnVykPsU2nS81_5Qn-CgTb-v0b8bzeLQqMlggGJ_d1hFmtt3fyM6om-s7h8rGatRbkmqgz9dcmepJGI7PIDZP4LKKFsmm66vFdyxFJzPFR3IqsqhDaATpZtvZz05675qUPUxQQg3x9kwrSdTWlDNiXaN1dMQdtFTTIStBdeNm4BBOHWb0FL974Vr8dcqaqo9Xy3g";
		Map<String, Object> response = webClient.get()
				.uri("http://localhost:{port}/v1/debts", port)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange()
				.expectStatus().isUnauthorized()
				.expectBody(ResolvableType.forClassWithGenerics(Map.class, String.class,
						Object.class))
				.value().<Map<String, Object>> returnResult().getResponseBody();
		assertThat(response).containsEntry("error", "invalid_token")
				.containsEntry("error_description", "Invalid access token: " + token);
	}

	@Test
	public void invalidExpired() {
		String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiMDAwMDAwMDAtMDAwMC0wMDAwLTAwMDAtMDAwMDAwMDAwMDAwIiwidXNlcl9uYW1lIjoibWFraUBleGFtcGxlLmNvbSIsInNjb3BlIjpbInJlYWQiLCJ3cml0ZSJdLCJleHAiOjE0ODg0NTAxODYsImRpc3BsYXlfbmFtZSI6Ik1ha2kgVG9zaGlha2kiLCJqdGkiOiIwZWU2Y2M4Zi1jYTIwLTRjNmItOTRkYS1hMzM0YzJlMjU2YTYiLCJlbWFpbCI6Im1ha2lAZXhhbXBsZS5jb20iLCJjbGllbnRfaWQiOiJmb28ifQ.JBuD-TBJMqZUNTqZYfOQu17OcWoHeIzhXFZitVuNgsROirnfX8l2wTSPkff2wHq379p2kwv1o6SMH20ZE4JLEf2aaVny_iWcV9Z7XjOiW0HribeLzg-uDqQUzctHQK9JXtCKuUMAR6lwBx5-B00qH_0talTo2-q6rbcAAT0SYqelUDhfeDqPqW5vx_oquQDBr9ZWsDzw5HZy890cF0IpoGod480vMjLZ1KcIdWP-G7wPjVrmsoIVvGy45nRwMws0-kKfymOn0WOfEyesfVjGq1Q47Z9ZXIZRrRs_IqU5_pYRwmXM-JoM6ya5sSxiBzkKbkjOcaj3f7eCXVRocbbSRA";
		Map<String, Object> response = webClient.get()
				.uri("http://localhost:{port}/v1/debts", port)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange()
				.expectStatus().isUnauthorized()
				.expectBody(ResolvableType.forClassWithGenerics(Map.class, String.class,
						Object.class))
				.value().<Map<String, Object>> returnResult().getResponseBody();
		assertThat(response).containsEntry("error", "invalid_token")
				.containsEntry("error_description", "Access token expired: " + token);
	}
}
