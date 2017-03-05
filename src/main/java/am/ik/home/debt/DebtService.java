package am.ik.home.debt;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebtService {
	private final DebtRedisRepository debtRepository;

	public Mono<Debt> findOne(UUID debtId) {
		return this.debtRepository.findOne(debtId);
	}

	public Flux<Debt> findAll() {
		return this.debtRepository.findAll();
	}

	public Flux<Debt> findByFrom(String from) {
		return findAll().filter(dept -> Objects.equals(from, dept.getFrom()));
	}

	public Flux<Debt> findByTo(String to) {
		return findAll().filter(dept -> Objects.equals(to, dept.getTo()));
	}

	public Mono<Debt> save(Debt debt) {
		debt.setDebtId(UUID.randomUUID());
		debt.setCreatedAt(Instant.now());
		if (debt.getDebtDate() == null) {
			debt.setDebtDate(LocalDate.now());
		}
		debt.setRepayments(new ArrayList<>());
		return this.debtRepository.save(debt).then(Mono.just(debt));
	}

	public Mono<Debt> repay(UUID deptId, DebtRepayment repayment) {
		return findOne(deptId).then(dept -> checkClear(dept, repayment)
				.filter(cleared -> !cleared).then(cleared -> {
					if (repayment.getRepaymentDate() == null) {
						repayment.setRepaymentDate(LocalDate.now());
					}
					repayment.setDebt(dept);
					repayment.setCreatedAt(Instant.now());
					dept.getRepayments().add(repayment);
					return this.debtRepository.delete(deptId).then(
							v -> this.debtRepository.save(dept).then(Mono.just(dept)));
				})).otherwiseIfEmpty(Mono.empty());
	}

	private Mono<Boolean> checkClear(Debt debt, DebtRepayment repayment) {
		return this.debtRepository.contains(DebtClear.builder().debt(debt).build())
				.then(x -> {
					if (x) {
						return Mono.error(new IllegalStateException(
								"The given dept has been cleared."));
					}
					long total = debt.getRepayments().stream()
							.map(DebtRepayment::getAmount).mapToLong(Long::longValue)
							.sum();
					long remain = debt.getAmount() - (total + repayment.getAmount());
					if (remain <= 0) {
						log.info("Clear dept({})", debt.getDebtId());
						return this.debtRepository
								.save(DebtClear.builder().debt(debt)
										.clearDate(LocalDate.now())
										.createdAt(Instant.now()).build())
								.then(v -> this.debtRepository.delete(debt.getDebtId()))
								.then(v -> Mono.just(true));
					}
					return Mono.just(false);
				});
	}
}
