package am.ik.home.security;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.InvalidSignatureException;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import am.ik.home.client.user.UaaUser;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class JwtOAuth2WebFilter implements WebFilter {
	private final RsaVerifier verifier;
	private final ObjectMapper objectMapper;
	private static final int BEARER_STRLEN = "bearer ".length();
	private final DataBufferFactory bufferFactory;
	private final AuthorizationCodeFlow authorizationCodeFlow;

	public JwtOAuth2WebFilter(RsaVerifier verifier, ObjectMapper objectMapper,
			AuthorizationCodeFlow authorizationCodeFlow) {
		this.verifier = verifier;
		this.objectMapper = objectMapper;
		this.authorizationCodeFlow = authorizationCodeFlow;
		this.bufferFactory = new DefaultDataBufferFactory(true);
	}

	boolean isAuthorizedApiRequest(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		String path = request.getURI().getPath();
		return path.startsWith("/v1");
	}

	boolean isLoginRequest(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		String path = request.getURI().getPath();
		return path.startsWith("/login");
	}

	boolean isDocsRequest(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		String path = request.getURI().getPath();
		return path.startsWith("/docs");
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		if (isDocsRequest(exchange)) {
			return chain.filter(exchange);
		}
		if (!isAuthorizedApiRequest(exchange)) {
			return authorize(exchange, chain);
		}
		HttpHeaders headers = exchange.getRequest().getHeaders();
		String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
		if (authorization != null && authorization.length() > BEARER_STRLEN
				&& authorization.substring(0, BEARER_STRLEN)
						.equalsIgnoreCase("bearer ")) {
			String token = authorization.substring(BEARER_STRLEN);
			return user(token)
					.flatMap(user -> chain
							.filter(exchange.mutate().principal(Mono.just(user)).build()))
					.onErrorResume(TokenExpiredException.class,
							t -> expired(exchange, token))
					.onErrorResume(InvalidSignatureException.class,
							t -> invalidToken(exchange, token))
					.onErrorResume(IllegalArgumentException.class,
							t -> invalidToken(exchange, token));
		}
		else {
			return unauthorized(exchange, "unauthorized",
					"Full authentication is required to access this resource");
		}
	}

	Mono<Void> authorize(ServerWebExchange exchange, WebFilterChain chain) {
		return exchange.getSession().doOnNext(s -> {
			if (!s.isStarted()) {
				s.start();
			}
		}).flatMap(s -> {
			if (isLoginRequest(exchange)) {
				return authorizationCodeFlow.token(exchange);
			}
			else {
				return exchange.getSession()
						.map(session -> session.getAttribute(AccessToken.ATTRIBUTE_NAME))
						.map(Optional::get).cast(AccessToken.class)
						.filter(token -> !token.isExpired())
						.flatMap(token -> chain.filter(exchange.mutate()
								.principal(user(token.getValue())).build()))
						.onErrorResume(NoSuchElementException.class,
								e -> authorizationCodeFlow.authorize(exchange));
			}
		});
	}

	Mono<Principal> user(String token) {
		try {
			Jwt jwt = JwtHelper.decodeAndVerify(token, verifier);
			JsonNode node = objectMapper.readValue(jwt.getClaims(), JsonNode.class);
			Instant exp = Instant.ofEpochSecond(node.get("exp").asLong());
			if (exp.isBefore(Instant.now())) {
				return Mono.error(new TokenExpiredException(
						String.format("Token expired (expiration=%s) : %s", exp, token)));
			}
			return Mono.just(new UserPrincipal(new UaaUser(objectMapper, token), token));
		}
		catch (Exception e) {
			return Mono.error(e);
		}
	}

	Mono<Void> invalidToken(ServerWebExchange exchange, String token) {
		String errorDescription = String.format("Invalid access token: %s", token);
		return unauthorized(exchange, "invalid_token", errorDescription);
	}

	Mono<Void> expired(ServerWebExchange exchange, String token) {
		String errorDescription = String.format("Access token expired: %s", token);
		return unauthorized(exchange, "invalid_token", errorDescription);
	}

	Mono<Void> unauthorized(ServerWebExchange exchange, String error,
			String errorDescription) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.UNAUTHORIZED);
		response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE,
				"Bearer realm=\"oauth2-resource\", unauthorized=\"invalid_token\", error_description=\""
						+ errorDescription + "\"");
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		String json = String.format("{\"error\":\"%s\",\"error_description\":\"%s\"}",
				error, errorDescription);
		return response.writeWith(Mono
				.just(this.bufferFactory.wrap(json.getBytes(StandardCharsets.UTF_8))));
	}

	static class TokenExpiredException extends RuntimeException {
		TokenExpiredException(String message) {
			super(message);
		}
	}
}
