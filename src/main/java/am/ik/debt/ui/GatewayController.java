package am.ik.debt.ui;

import static org.springframework.web.reactive.function.BodyInserters.fromPublisher;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import am.ik.debt.ApiProps;
import am.ik.debt.security.UserPrincipal;
import am.ik.home.client.member.Member;
import reactor.core.publisher.Flux;
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

	@GetMapping("gateway")
	Mono<Map<String, Object>> gateway(UserPrincipal principal) {
		Mono<JsonNode> debts = this.webClient.get()
				.uri(props.getExternalUrl() + "/v1/debts")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + principal.getToken())
				.exchange().then(x -> x.bodyToMono(JsonNode.class));
		Mono<JsonNode> members = this.webClient.get()
				.uri(props.getAuthorizationUrl() + "/v1/members")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + principal.getToken())
				.exchange().then(x -> x.bodyToMono(JsonNode.class));
		return Mono.when(debts, members).map(t -> {
			Map<String, Object> response = new LinkedHashMap<>();
			response.put("me", principal.getUaaUser());
			response.put("debts", t.getT1());
			response.put("members", t.getT2().get("_embedded").get("members"));
			return response;
		});
	}

	@PostMapping("debts")
	Mono<JsonNode> postDebts(@RequestBody Mono<JsonNode> body, UserPrincipal principal) {
		return this.webClient.post().uri(props.getExternalUrl() + "/v1/debts")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + principal.getToken())
				.exchange(fromPublisher(body, JsonNode.class))
				.then(x -> x.bodyToMono(JsonNode.class));
	}

	@PostMapping("debts/{debtId}/repayments")
	Mono<JsonNode> postRepayments(@RequestBody Mono<JsonNode> body,
			@PathVariable String debtId, UserPrincipal principal) {
		return this.webClient.post()
				.uri(props.getExternalUrl() + "/v1/debts/{debtId}/repayments", debtId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + principal.getToken())
				.exchange(fromPublisher(body, JsonNode.class))
				.then(x -> x.bodyToMono(JsonNode.class));
	}

	Flux<Member> deserializeMembers(String s) {
		try {
			return Flux.fromIterable(
					objectMapper.readValue(s, new TypeReference<List<Member>>() {

					}));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
