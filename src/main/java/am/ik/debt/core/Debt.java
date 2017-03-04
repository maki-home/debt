package am.ik.debt.core;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(exclude = "repayments")
@Builder
@NoArgsConstructor
public class Debt implements Serializable {
	private UUID debtId;
	private String purpose;
	private Long amount;
	private String from;
	private String to;
	private List<DebtRepayment> repayments = new ArrayList<>();
	@JsonFormat(pattern = "uuuu-MM-dd")
	private LocalDate debtDate;
	private Instant createdAt;

	private Debt(UUID debtId, String purpose, Long amount, String from, String to,
			List<DebtRepayment> repayments, LocalDate debtDate, Instant createdAt) {
		this.debtId = debtId;
		this.purpose = purpose;
		this.amount = amount;
		this.from = from;
		this.to = to;
		this.repayments = repayments;
		this.debtDate = debtDate;
		this.createdAt = createdAt;
	}
}
