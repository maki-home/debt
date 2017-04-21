package am.ik.home.debt;

import static java.util.Collections.emptyMap;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@Slf4j
public class DebtRedisRepository implements DebtRepository {
	private final ReactiveRedisConnection connection;
	private final Jackson2JsonDecoder jsonDecoder;
	private final Jackson2JsonEncoder jsonEncoder;
	private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory(true);

	private static final String DEBT_PREFIX = "debt:";
	private static final String DEBT_CLEAR = "debt_clear:";

	public DebtRedisRepository(ReactiveRedisConnectionFactory connectionFactory,
			ObjectMapper objectMapper) {
		log.info("set up DebtRedisRepository with {}", connectionFactory);
		this.connection = connectionFactory.getReactiveConnection();
		this.jsonDecoder = new Jackson2JsonDecoder(objectMapper);
		this.jsonEncoder = new Jackson2JsonEncoder(objectMapper);
	}

	@Override
	public Mono<Boolean> contains(DebtClear clear) {
		ByteBuffer clearKey = clearKey(clear);
		return this.connection.keyCommands().exists(clearKey);
	}

	@Override
	public Mono<Long> save(Debt debt) {
		ByteBuffer debtKey = debtKey(debt);
		return this.connection.keyCommands().exists(debtKey)
				.flatMap(x -> x ? Mono.just(1L)
						: this.encode(Mono.just(debt), Debt.class).collectList().flatMap(
								v -> this.connection.listCommands().rPush(debtKey, v)));
	}

	@Override
	public Mono<Long> save(DebtClear clear) {
		ByteBuffer clearKey = clearKey(clear);
		return this.connection.keyCommands().exists(clearKey).flatMap(x -> x
				? Mono.empty()
				: this.encode(Mono.just(clear), DebtClear.class).collectList()
						.flatMap(v -> this.connection.listCommands().rPush(clearKey, v)));
	}

	@Override
	public Mono<Long> delete(UUID debtId) {
		ByteBuffer debtKey = debtKey(debtId);
		return this.connection.keyCommands().del(debtKey);
	}

	@Override
	public Mono<Debt> findOne(UUID debtId) {
		return this.getDebt(debtKey(debtId));
	}

	private Mono<Debt> getDebt(ByteBuffer key) {
		Flux<ByteBuffer> range = this.connection.listCommands().lRange(key, 0, -1);
		return this.decode(range, Debt.class);
	}

	@Override
	public Flux<Debt> findAll() {
		return this.connection.keyCommands().keys(wrap(DEBT_PREFIX + "*"))
				.flatMapMany(Flux::fromIterable).flatMap(this::getDebt);
	}

	private <T> Flux<ByteBuffer> encode(Mono<T> obj, Class<T> clazz) {
		return this.jsonEncoder.encode(obj, bufferFactory, ResolvableType.forClass(clazz),
				APPLICATION_JSON, emptyMap()).map(DataBuffer::asByteBuffer);
	}

	private <T> Mono<T> decode(Publisher<ByteBuffer> buf, Class<T> clazz) {
		return this.jsonDecoder
				.decodeToMono(Flux.from(buf).map(bufferFactory::wrap),
						ResolvableType.forClass(clazz), APPLICATION_JSON, emptyMap())
				.map(clazz::cast);
	}

	private static ByteBuffer debtKey(Debt debt) {
		return debtKey(debt.getDebtId());
	}

	private static ByteBuffer debtKey(UUID debtId) {
		return wrap(DEBT_PREFIX + debtId);
	}

	private static ByteBuffer clearKey(DebtClear clear) {
		return wrap(DEBT_CLEAR + clear.getDebt().getDebtId());
	}

	private static ByteBuffer wrap(String s) {
		return ByteBuffer.wrap(s.getBytes());
	}
}
