package com.smartload.optimizer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;

public record ObjectiveWeightsDto(
		@DecimalMin(value = "0.0", inclusive = true)
		double payout,
		@JsonProperty("weight_utilization")
		@DecimalMin(value = "0.0", inclusive = true)
		double weightUtilization,
		@JsonProperty("volume_utilization")
		@DecimalMin(value = "0.0", inclusive = true)
		double volumeUtilization
) {
	public double sum() {
		return payout + weightUtilization + volumeUtilization;
	}
}
