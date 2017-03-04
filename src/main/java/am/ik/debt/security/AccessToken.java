package am.ik.debt.security;

import java.io.Serializable;
import java.time.Instant;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Builder
@Data
public class AccessToken implements Serializable {
	public static final String ATTRIBUTE_NAME = AccessToken.class.getName();
	private final String value;
	private final Instant expiration;

	public boolean isExpired() {
		return Instant.now().isAfter(expiration);
	}
}
