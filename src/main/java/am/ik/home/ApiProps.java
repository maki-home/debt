package am.ik.home;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@ConfigurationProperties(prefix = "api")
@Data
@Component
public class ApiProps {
	private String authorizationUrl;
	private String debtUrl;
	private String externalUrl;
}
