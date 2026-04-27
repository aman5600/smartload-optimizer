package com.smartload.optimizer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record ApiError(
		Instant timestamp,
		int status,
		String error,
		String message,
		@JsonProperty("path")
		String requestPath
) {
}
