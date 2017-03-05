package am.ik.debt;

import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import java.util.Map;

import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {
	@Bean
	WebClient webClient() {
		return WebClient.builder()
				.defaultHeader(HttpHeaders.USER_AGENT, "am.ik.debt.DebtApplication")
				.build();
	}

	@Bean
	RsaVerifier rsaVerifier(WebClient webClient, ResourceServerProperties resource) {
		String username = resource.getClientId();
		String password = resource.getClientSecret();
		return webClient.filter(basicAuthentication(username, password)).get()
				.uri(resource.getJwt().getKeyUri()).exchange()
				.then(x -> x.bodyToMono(Map.class))
				.map(key -> new RsaVerifier(key.get("value").toString())).block();
	}

	@Profile("!cloud")
	@Bean
	RedisConnectionFactory redisConnectionFactory() {
		return new LettuceConnectionFactory();
	}
}
