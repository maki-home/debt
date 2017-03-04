package am.ik.debt.core;

import java.util.UUID;

import org.springframework.web.bind.annotation.*;

import am.ik.debt.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class DebtRestController {
	private final DebtService debtService;

	@GetMapping(path = "v1/debts")
	Flux<Debt> getDebts() {
		return debtService.findAll();
	}

	@GetMapping(path = "v1/debts", params = "from")
	Flux<Debt> getDebtsByFrom(@RequestParam String from) {
		return debtService.findByFrom(from);
	}

	@GetMapping(path = "v1/debts", params = "from=me")
	Flux<Debt> getDebtsByFromMe(UserPrincipal principal) {
		String from = principal.getUaaUser().getUserId();
		return debtService.findByFrom(from);
	}

	@GetMapping(path = "v1/debts", params = "to")
	Flux<Debt> getDebtsByTo(@RequestParam String to) {
		return debtService.findByTo(to);
	}

	@GetMapping(path = "v1/debts", params = "to=me")
	Flux<Debt> getDebtsByToMe(UserPrincipal principal) {
		String to = principal.getUaaUser().getUserId();
		return debtService.findByTo(to);
	}

	@GetMapping(path = "v1/debts/{debtId}")
	Mono<Debt> getDebts(@PathVariable UUID debtId) {
		return debtService.findOne(debtId);
	}

	@PostMapping(path = "v1/debts")
	Mono<Debt> postDebts(@RequestBody Mono<Debt> debt) {
		return debt.then(d -> debtService.save(d));
	}

	@PostMapping(path = "v1/debts", params = "from=me")
	Mono<Debt> postDebtsFromMe(@RequestBody Mono<Debt> debt, UserPrincipal principal) {
		return debt.then(d -> {
			d.setFrom(principal.getUaaUser().getUserId());
			return debtService.save(d);
		});
	}

	@PostMapping(path = "v1/debts", params = "to=me")
	Mono<Debt> postDebtsToMe(@RequestBody Mono<Debt> debt, UserPrincipal principal) {
		return debt.then(d -> {
			d.setTo(principal.getUaaUser().getUserId());
			return debtService.save(d);
		});
	}
}
