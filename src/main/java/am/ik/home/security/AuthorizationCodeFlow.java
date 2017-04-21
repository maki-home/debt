package am.ik.home.security;

import java.net.URI;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import am.ik.home.ApiProps;
import reactor.core.publisher.Mono;

@Component
public class AuthorizationCodeFlow {

	private final AuthorizationCodeResourceDetails details;
	private final WebClient webClient;
	private final ApiProps apiProps;

	public AuthorizationCodeFlow(AuthorizationCodeResourceDetails details,
			ApiProps apiProps) {
		this.details = details;
		this.webClient = WebClient.create(details.getAccessTokenUri());
		this.apiProps = apiProps;
	}

	public Mono<Void> authorize(ServerWebExchange exchange) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.FOUND);
		HttpHeaders headers = response.getHeaders();
		UriComponentsBuilder builder = UriComponentsBuilder
				.fromHttpUrl(details.getUserAuthorizationUri())
				.queryParam("response_type", "code")
				.queryParam("client_id", details.getClientId())
				.queryParam("redirect_uri", apiProps.getExternalUrl() + "/login")
				.queryParam("scope", String.join(" ", details.getScope()));
		headers.setLocation(builder.build().toUri());
		return Mono.empty();
	}

	public Mono<Void> token(ServerWebExchange exchange) {
		String code = exchange.getRequest().getQueryParams().getFirst("code");
		if (StringUtils.isEmpty(code)) {
			ServerHttpResponse response = exchange.getResponse();
			response.setStatusCode(HttpStatus.BAD_REQUEST);
			return Mono.empty();
		}
		String username = details.getClientId();
		String password = details.getClientSecret();
		byte[] basic = Base64.encode((username + ":" + password).getBytes());
		return webClient.post()
				.uri(b -> b.queryParam("grant_type", "authorization_code")
						.queryParam("code", code)
						.queryParam("redirect_uri", apiProps.getExternalUrl() + "/login")
						.queryParam("scope", String.join(" ", details.getScope()))
						.build())
				.header(HttpHeaders.AUTHORIZATION, "Basic " + new String(basic))
				.exchange().flatMap(x -> x.bodyToMono(JsonNode.class)).flatMap(node -> {
					String token = node.get("access_token").asText();
					Long expiresIn = node.get("expires_in").asLong();
					AccessToken accessToken = AccessToken.builder().value(token)
							.expiration(Instant.now().plusSeconds(expiresIn)).build();
					return exchange.getSession().doOnNext(s -> s.getAttributes()
							.put(AccessToken.ATTRIBUTE_NAME, accessToken));
				}).flatMap(x -> {
					ServerHttpResponse response = exchange.getResponse();
					response.setStatusCode(HttpStatus.FOUND);
					response.getHeaders().setLocation(
							URI.create(apiProps.getExternalUrl() + "/index.html"));
					return Mono.empty();
				});
	}
}
