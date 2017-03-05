package am.ik.debt;

import org.springframework.cloud.config.java.AbstractCloudConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Profile("cloud")
public class CloudConfig extends AbstractCloudConfig {
	@Bean
	RedisConnectionFactory redisConnectionFactory() {
		return redisConnectionFactory();
	}
}
