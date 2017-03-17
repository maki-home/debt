package am.ik.home.exception;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

@RestControllerAdvice
public class RestExceptionHandler {
	private final MessageSourceAccessor messageSourceAccessor;

	public RestExceptionHandler(MessageSource messageSource) {
		this.messageSourceAccessor = new MessageSourceAccessor(messageSource,
				Locale.getDefault());
	}

	@ExceptionHandler(WebExchangeBindException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ConstraintViolationExceptionMessage handleException(
			WebExchangeBindException e) {
		return new ConstraintViolationExceptionMessage(e,
				messageSourceAccessor);
	}
}
