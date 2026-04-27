package com.smartload.optimizer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record TruckDto(
		@NotBlank String id,
		@JsonProperty("max_weight_lbs")
		@Min(0) long maxWeightLbs,
		@JsonProperty("max_volume_cuft")
		@Min(0) long maxVolumeCuft
) {
}
