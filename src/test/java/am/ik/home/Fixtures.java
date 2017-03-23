package am.ik.home;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import am.ik.home.debt.Debt;
import am.ik.home.debt.DebtRepayment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Fixtures {

	public static Mono<Debt> debt() {
		return debts().elementAt(0).map(d -> {
			d.setDebtId(null);
			return d;
		});
	}

	public static Flux<Debt> debts() {
		return Flux.just(
				Debt.builder()
						.debtId(UUID.fromString("10000000-0000-0000-0000-000000000000"))
						.purpose("purpose1").from("00000000-0000-0000-0000-000000000000")
						.to("00000000-0000-0000-0000-000000000001").amount(1000L)
						.createdAt(Instant.now())
						.repayments(asList(
								DebtRepayment.builder().amount(500L)
										.createdAt(Instant.now())
										.repaymentDate(LocalDate.now()).build(),
								DebtRepayment.builder().amount(300L)
										.createdAt(Instant.now()).repaymentDate(
												LocalDate.now())
										.build()))
						.debtDate(LocalDate.now()).build(),
				Debt.builder()
						.debtId(UUID.fromString("10000000-0000-0000-0000-000000000001"))
						.purpose("purpose2").from("00000000-0000-0000-0000-000000000001")
						.to("00000000-0000-0000-0000-000000000002").amount(2000L)
						.createdAt(Instant.now()).repayments(emptyList())
						.debtDate(LocalDate.now()).build(),
				Debt.builder()
						.debtId(UUID.fromString("10000000-0000-0000-0000-000000000002"))
						.purpose("purpose3")
						.repayments(singletonList(DebtRepayment.builder().amount(500L)
								.createdAt(Instant.now()).repaymentDate(LocalDate.now())
								.build()))
						.from("00000000-0000-0000-0000-000000000002")
						.to("00000000-0000-0000-0000-000000000000").amount(3000L)
						.createdAt(Instant.now()).debtDate(LocalDate.now()).build());
	}
}
