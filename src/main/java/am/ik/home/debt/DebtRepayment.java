package am.ik.home.debt;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class DebtRepayment implements Serializable {
	@Getter(onMethod = @__(@JsonIgnore))
	private Debt debt;
	private Long amount;
	@JsonFormat(pattern = "uuuu-MM-dd")
	private LocalDate repaymentDate;
	private Instant createdAt;

	private DebtRepayment(Debt debt, Long amount, LocalDate repaymentDate,
			Instant createdAt) {
		this.debt = debt;
		this.amount = amount;
		this.repaymentDate = repaymentDate;
		this.createdAt = createdAt;
	}

	@Override
	public String toString() {
		return "DebtRepayment{" + "debtId=" + (debt == null ? "null" : debt.getDebtId())
				+ ", amount=" + amount + ", repaymentDate=" + repaymentDate
				+ ", createdAt=" + createdAt + '}';
	}
}
