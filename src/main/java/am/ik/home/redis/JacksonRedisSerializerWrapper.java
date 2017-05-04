package am.ik.home.redis;

import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonRedisSerializerWrapper<T>
		implements org.springframework.data.redis.serializer.RedisSerializer<T> {
	private final Class<T> clazz;
	private final ObjectMapper objectMapper;
	private final GenericJackson2JsonRedisSerializer delegate;

	public JacksonRedisSerializerWrapper(Class<T> clazz, ObjectMapper objectMapper) {
		this.clazz = clazz;
		this.objectMapper = objectMapper;
		this.delegate = new GenericJackson2JsonRedisSerializer(objectMapper);
	}

	@Override
	public byte[] serialize(T t) throws SerializationException {
		return delegate.serialize(t);
	}

	@Override
	public T deserialize(byte[] bytes) throws SerializationException {
		return clazz.cast(delegate.deserialize(bytes));
	}
}
