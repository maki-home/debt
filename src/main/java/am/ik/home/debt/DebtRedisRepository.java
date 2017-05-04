package am.ik.home.debt;

import java.util.UUID;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@Slf4j
public class DebtRedisRepository implements DebtRepository {
	private static final String DEBT_PREFIX = "debt:";
	private static final String DEBT_CLEAR = "debt_clear:";

	private final ReactiveRedisTemplate<Object, Object> redisTemplate;
	private final RedisSerializationContext<String, Debt> debtSerializationContext;
	private final RedisSerializationContext<String, DebtClear> debtClearSerializationContext;

	public DebtRedisRepository(ReactiveRedisTemplate<Object, Object> redisTemplate,
			ObjectMapper objectMapper) {
		RedisSerializer<String> keySerializer = new StringRedisSerializer();
		Jackson2JsonRedisSerializer<Debt> debtSerializer = new Jackson2JsonRedisSerializer<>(
				Debt.class);
		debtSerializer.setObjectMapper(objectMapper);
		Jackson2JsonRedisSerializer<DebtClear> debtClearSerializer = new Jackson2JsonRedisSerializer<>(
				DebtClear.class);
		debtClearSerializer.setObjectMapper(objectMapper);
		this.redisTemplate = redisTemplate;
		this.debtSerializationContext = RedisSerializationContext
				.<String, Debt>newSerializationContext().key(keySerializer)
				.value(debtSerializer).hashKey(debtSerializer).hashValue(debtSerializer)
				.build();
		this.debtClearSerializationContext = RedisSerializationContext
				.<String, DebtClear>newSerializationContext().key(keySerializer)
				.value(debtClearSerializer).hashKey(debtClearSerializer)
				.hashValue(debtClearSerializer).build();
	}

	@Override
	public Mono<Boolean> contains(DebtClear clear) {
		String clearKey = clearKey(clear);
		return redisTemplate.hasKey(clearKey);
	}

	@Override
	public Mono<Long> save(Debt debt) {
		String debtKey = debtKey(debt);
		return redisTemplate.hasKey(debtKey)
				.flatMap(x -> x ? Mono.empty()
						: redisTemplate.opsForList(debtSerializationContext)
								.rightPush(debtKey, debt));
	}

	@Override
	public Mono<Long> save(DebtClear clear) {
		String clearKey = clearKey(clear);
		return redisTemplate.hasKey(clearKey)
				.flatMap(x -> x ? Mono.empty()
						: redisTemplate.opsForList(debtClearSerializationContext)
								.rightPush(clearKey, clear));
	}

	@Override
	public Mono<Long> delete(UUID debtId) {
		String debtKey = debtKey(debtId);
		return redisTemplate.delete(debtKey);
	}

	@Override
	public Mono<Debt> findOne(UUID debtId) {
		return redisTemplate.opsForValue(debtSerializationContext).get(debtKey(debtId));
	}

	@Override
	public Flux<Debt> findAll() {
		return redisTemplate.keys(DEBT_PREFIX + "*")
				.flatMap(k -> redisTemplate.opsForValue(debtSerializationContext).get(k));
	}

	private static String debtKey(Debt debt) {
		return debtKey(debt.getDebtId());
	}

	private static String debtKey(UUID debtId) {
		return DEBT_PREFIX + debtId;
	}

	private static String clearKey(DebtClear clear) {
		return DEBT_CLEAR + clear.getDebt().getDebtId();
	}

}
