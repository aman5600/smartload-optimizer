package com.smartload.optimizer.model;

import java.util.List;

public record CandidateSolution(
		List<Integer> selectedIndexes,
		long totalPayoutCents,
		long totalWeightLbs,
		long totalVolumeCuft
) {
}
