package am.ik.debt.core;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DebtClear implements Serializable {
	private Debt debt;
	@JsonFormat(pattern = "uuuu-MM-dd")
	private LocalDate clearDate;
	private Instant createdAt;
}
