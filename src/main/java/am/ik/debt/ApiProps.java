package am.ik.debt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@ConfigurationProperties(prefix = "api")
@Data
@Component
public class ApiProps {
	private String authorizationUrl;
	private String externalUrl;
}
