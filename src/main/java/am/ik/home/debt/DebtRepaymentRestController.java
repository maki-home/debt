package am.ik.home.debt;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
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
			@Validated @RequestBody Mono<DebtRepayment> repayment) {
		return repayment.flatMap(r -> debtService.repay(debtId, r));
	}
}
