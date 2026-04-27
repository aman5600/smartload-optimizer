package com.smartload.optimizer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record OrderDto(
		@NotBlank String id,
		@JsonProperty("payout_cents")
		@Min(0) long payoutCents,
		@JsonProperty("weight_lbs")
		@Min(0) long weightLbs,
		@JsonProperty("volume_cuft")
		@Min(0) long volumeCuft,
		@NotBlank String origin,
		@NotBlank String destination,
		@JsonProperty("pickup_date")
		@NotNull LocalDate pickupDate,
		@JsonProperty("delivery_date")
		@NotNull LocalDate deliveryDate,
		@JsonProperty("is_hazmat")
		boolean isHazmat
) {
}
