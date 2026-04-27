package com.smartload.optimizer.exception;

import com.smartload.optimizer.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(PayloadTooLargeException.class)
	public ResponseEntity<ApiError> handlePayloadTooLarge(PayloadTooLargeException ex, HttpServletRequest request) {
		return buildError(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage(), request);
	}

	@ExceptionHandler({BadRequestException.class, MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
	public ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest request) {
		if (ex instanceof MethodArgumentNotValidException validationException) {
			StringBuilder builder = new StringBuilder("Validation failed");
			for (FieldError fieldError : validationException.getBindingResult().getFieldErrors()) {
				builder.append("; ").append(fieldError.getField()).append(": ").append(fieldError.getDefaultMessage());
			}
			return buildError(HttpStatus.BAD_REQUEST, builder.toString(), request);
		}
		return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
		return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected internal error", request);
	}

	private ResponseEntity<ApiError> buildError(HttpStatus status, String message, HttpServletRequest request) {
		ApiError payload = new ApiError(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getRequestURI()
		);
		return ResponseEntity.status(status).body(payload);
	}
}
