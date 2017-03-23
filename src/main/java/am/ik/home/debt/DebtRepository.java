package am.ik.home.debt;

import java.util.UUID;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DebtRepository {
	Mono<Boolean> contains(DebtClear clear);

	Mono<Long> save(Debt debt);

	Mono<Long> save(DebtClear clear);

	Mono<Long> delete(UUID debtId);

	Mono<Debt> findOne(UUID debtId);

	Flux<Debt> findAll();
}
