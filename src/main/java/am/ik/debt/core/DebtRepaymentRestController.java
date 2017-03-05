package am.ik.debt.core;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class DebtRepaymentRestController {
	private final DebtService debtService;

	@PostMapping(path = "v1/debts/{debtId}/repayments")
	@ResponseStatus(HttpStatus.CREATED)
	Mono<Debt> postDebts(@PathVariable UUID debtId,
			@RequestBody Mono<DebtRepayment> repayment) {
		return repayment.then(r -> debtService.repay(debtId, r));
	}
}
