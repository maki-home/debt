package am.ik.debt;

import java.util.Map;

import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {
	@Bean
	WebClient webClient() {
		return WebClient.create();
	}

	@Bean
	RsaVerifier rsaVerifier(WebClient webClient, ResourceServerProperties resource) {
		String username = resource.getClientId();
		String password = resource.getClientSecret();
		byte[] token = Base64.encode((username + ":" + password).getBytes());
		return webClient.get().uri(resource.getJwt().getKeyUri())
				.header("Authorization", "Basic " + new String(token)).exchange()
				.then(x -> x.bodyToMono(Map.class))
				.map(key -> new RsaVerifier(key.get("value").toString())).block();
	}

	@Bean
	RedisConnectionFactory redisConnectionFactory() {
		return new LettuceConnectionFactory();
	}
}
