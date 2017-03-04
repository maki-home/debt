package am.ik.debt.core;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class DebtRepaymentRestController {
	private final DebtService debtService;

	@PostMapping(path = "v1/debts/{debtId}/repayments")
	Mono<Debt> postDebts(@PathVariable UUID debtId,
			@RequestBody Mono<DebtRepayment> repayment) {
		return repayment.then(r -> debtService.repay(debtId, r));
	}
}
