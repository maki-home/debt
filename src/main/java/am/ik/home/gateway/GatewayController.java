package am.ik.home.gateway;

import static org.springframework.web.reactive.function.BodyInserters.fromPublisher;
import static org.springframework.web.reactive.function.client.ClientRequest.from;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunction.ofRequestProcessor;

import java.util.LinkedHashMap;
import java.util.Map;

import am.ik.home.ApiProps;
import am.ik.home.security.UserPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@RestController
public class GatewayController {
	private final WebClient webClient;
	private final ObjectMapper objectMapper;
	private final ApiProps props;

	public GatewayController(WebClient webClient, ObjectMapper objectMapper,
			ApiProps props) {
		this.webClient = webClient;
		this.objectMapper = objectMapper;
		this.props = props;
	}

	ExchangeFilterFunction gatewayFilter(UserPrincipal principal, HttpHeaders headers) {
		return ofRequestProcessor((request) -> Mono.just(from(request)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + principal.getToken())
				.header("X-B3-SpanId", headers.getFirst("X-B3-Spanid"))
				.header("X-B3-TraceId", headers.getFirst("X-B3-Traceid")).build()));
	}

	@GetMapping("gateway")
	Mono<Map<String, Object>> gateway(UserPrincipal principal,
			ServerWebExchange exchange) {
		Mono<JsonNode> debts = this.webClient
				.filter(gatewayFilter(principal, exchange.getRequest().getHeaders()))
				.get().uri(props.getDebtUrl() + "/v1/debts").exchange()
				.then(x -> x.bodyToMono(JsonNode.class));
		Mono<JsonNode> members = this.webClient
				.filter(gatewayFilter(principal, exchange.getRequest().getHeaders()))
				.get().uri(props.getAuthorizationUrl() + "/v1/members").exchange()
				.then(x -> x.bodyToMono(JsonNode.class));
		return Mono.when(debts, members).map(t -> {
			Map<String, Object> response = new LinkedHashMap<>();
			response.put("me", principal.getUaaUser());
			response.put("debts", t.getT1());
			response.put("members", t.getT2().get("_embedded").get("members"));
			return response;
		});
	}

	@PostMapping("debts")
	Mono<JsonNode> postDebts(@RequestBody Mono<JsonNode> body, UserPrincipal principal,
			ServerWebExchange exchange) {
		return this.webClient
				.filter(gatewayFilter(principal, exchange.getRequest().getHeaders()))
				.post().uri(props.getDebtUrl() + "/v1/debts")
				.exchange(fromPublisher(body, JsonNode.class))
				.then(x -> x.bodyToMono(JsonNode.class));
	}

	@PostMapping("debts/{debtId}/repayments")
	Mono<JsonNode> postRepayments(@RequestBody Mono<JsonNode> body,
			@PathVariable String debtId, UserPrincipal principal,
			ServerWebExchange exchange) {
		return this.webClient
				.filter(gatewayFilter(principal, exchange.getRequest().getHeaders()))
				.post().uri(props.getDebtUrl() + "/v1/debts/{debtId}/repayments", debtId)
				.exchange(fromPublisher(body, JsonNode.class))
				.then(x -> x.bodyToMono(JsonNode.class));
	}
}
