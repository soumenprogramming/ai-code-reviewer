package com.soumenprogramming.ai_code_reviewer.exception;

import com.soumenprogramming.ai_code_reviewer.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ApplicationException.class)
	public ResponseEntity<ApiErrorResponse> handleApplicationException(ApplicationException exception) {
		return ResponseEntity
			.status(exception.getStatus())
			.body(new ApiErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
		String message = exception.getReason() == null ? "Request failed." : exception.getReason();
		return ResponseEntity
			.status(exception.getStatusCode())
			.body(new ApiErrorResponse(message));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(new ApiErrorResponse("Unexpected server error. Please try again."));
	}
}
