package com.smartload.optimizer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OptimizeLoadResponse(
		@JsonProperty("truck_id")
		String truckId,
		@JsonProperty("selected_order_ids")
		List<String> selectedOrderIds,
		@JsonProperty("total_payout_cents")
		long totalPayoutCents,
		@JsonProperty("total_weight_lbs")
		long totalWeightLbs,
		@JsonProperty("total_volume_cuft")
		long totalVolumeCuft,
		@JsonProperty("utilization_weight_percent")
		double utilizationWeightPercent,
		@JsonProperty("utilization_volume_percent")
		double utilizationVolumePercent
) {
}
