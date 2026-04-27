package com.smartload.optimizer.service;

import com.smartload.optimizer.dto.OrderDto;
import com.smartload.optimizer.dto.ObjectiveWeightsDto;
import com.smartload.optimizer.dto.TruckDto;
import com.smartload.optimizer.model.CandidateSolution;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OptimizationEngine {
	@Autowired
	private CompatibilityService compatibilityService;

	public CandidateSolution optimize(TruckDto truck, List<OrderDto> orders, ObjectiveWeightsDto objectiveWeights) {
		int n = orders.size();
		if (n == 0) {
			return new CandidateSolution(List.of(), 0, 0, 0);
		}

		int totalMasks = 1 << n;
		long[] payout = new long[totalMasks];
		long[] weight = new long[totalMasks];
		long[] volume = new long[totalMasks];
		boolean[] compatible = new boolean[totalMasks];
		int[] representativeIndex = new int[totalMasks];
		LocalDate[] maxPickup = new LocalDate[totalMasks];
		LocalDate[] minDelivery = new LocalDate[totalMasks];

		compatible[0] = true;
		int bestMask = 0;

		for (int mask = 1; mask < totalMasks; mask++) {
			int lsb = mask & -mask;
			int idx = Integer.numberOfTrailingZeros(lsb);
			int previousMask = mask ^ lsb;

			payout[mask] = payout[previousMask] + orders.get(idx).payoutCents();
			weight[mask] = weight[previousMask] + orders.get(idx).weightLbs();
			volume[mask] = volume[previousMask] + orders.get(idx).volumeCuft();

			if (previousMask == 0) {
				compatible[mask] = true;
				representativeIndex[mask] = idx;
				maxPickup[mask] = orders.get(idx).pickupDate();
				minDelivery[mask] = orders.get(idx).deliveryDate();
			} else if (compatible[previousMask]) {
				int representative = representativeIndex[previousMask];
				OrderDto currentOrder = orders.get(idx);
				OrderDto representativeOrder = orders.get(representative);

				boolean laneAndHazmatCompatible =
						compatibilityService.sameLane(currentOrder, representativeOrder)
								&& compatibilityService.hazmatCompatible(currentOrder, representativeOrder);
				LocalDate candidateMaxPickup = maxPickup[previousMask].isAfter(currentOrder.pickupDate())
						? maxPickup[previousMask]
						: currentOrder.pickupDate();
				LocalDate candidateMinDelivery = minDelivery[previousMask].isBefore(currentOrder.deliveryDate())
						? minDelivery[previousMask]
						: currentOrder.deliveryDate();

				compatible[mask] = laneAndHazmatCompatible && !candidateMaxPickup.isAfter(candidateMinDelivery);
				representativeIndex[mask] = representative;
				maxPickup[mask] = candidateMaxPickup;
				minDelivery[mask] = candidateMinDelivery;
			}

			if (!compatible[mask]) {
				continue;
			}

			if (weight[mask] > truck.maxWeightLbs() || volume[mask] > truck.maxVolumeCuft()) {
				continue;
			}

			if (isBetterCandidate(mask, bestMask, payout, weight, volume, orders, truck, objectiveWeights)) {
				bestMask = mask;
			}
		}

		List<Integer> selectedIndexes = new ArrayList<>();
		for (int idx = 0; idx < n; idx++) {
			if ((bestMask & (1 << idx)) != 0) {
				selectedIndexes.add(idx);
			}
		}

		return new CandidateSolution(selectedIndexes, payout[bestMask], weight[bestMask], volume[bestMask]);
	}

	private boolean isBetterCandidate(
			int candidateMask,
			int bestMask,
			long[] payout,
			long[] weight,
			long[] volume,
			List<OrderDto> orders,
			TruckDto truck,
			ObjectiveWeightsDto objectiveWeights
	) {
		if (objectiveWeights != null) {
			double candidateScore = weightedObjectiveScore(candidateMask, payout, weight, volume, truck, objectiveWeights);
			double bestScore = weightedObjectiveScore(bestMask, payout, weight, volume, truck, objectiveWeights);
			if (Double.compare(candidateScore, bestScore) != 0) {
				return candidateScore > bestScore;
			}
		}

		if (payout[candidateMask] != payout[bestMask]) {
			return payout[candidateMask] > payout[bestMask];
		}

		long candidateWeightUtilScore = utilizationScore(weight[candidateMask], truck.maxWeightLbs());
		long bestWeightUtilScore = utilizationScore(weight[bestMask], truck.maxWeightLbs());
		if (candidateWeightUtilScore != bestWeightUtilScore) {
			return candidateWeightUtilScore > bestWeightUtilScore;
		}

		long candidateVolumeUtilScore = utilizationScore(volume[candidateMask], truck.maxVolumeCuft());
		long bestVolumeUtilScore = utilizationScore(volume[bestMask], truck.maxVolumeCuft());
		if (candidateVolumeUtilScore != bestVolumeUtilScore) {
			return candidateVolumeUtilScore > bestVolumeUtilScore;
		}

		return compareLexicographicIds(candidateMask, bestMask, orders) < 0;
	}

	private double weightedObjectiveScore(
			int mask,
			long[] payout,
			long[] weight,
			long[] volume,
			TruckDto truck,
			ObjectiveWeightsDto objectiveWeights
	) {
		double payoutRatio = payoutRatio(payout[mask], payout);
		double weightUtilization = utilizationFraction(weight[mask], truck.maxWeightLbs());
		double volumeUtilization = utilizationFraction(volume[mask], truck.maxVolumeCuft());
		return objectiveWeights.payout() * payoutRatio
				+ objectiveWeights.weightUtilization() * weightUtilization
				+ objectiveWeights.volumeUtilization() * volumeUtilization;
	}

	private double payoutRatio(long value, long[] payout) {
		long maxAvailablePayout = payout[payout.length - 1];
		if (maxAvailablePayout <= 0) {
			return 0.0;
		}
		return (double) value / maxAvailablePayout;
	}

	private double utilizationFraction(long used, long capacity) {
		if (capacity <= 0) {
			return 0.0;
		}
		return (double) used / capacity;
	}

	private long utilizationScore(long used, long capacity) {
		if (capacity <= 0) {
			return 0;
		}
		return used * 1_000_000L / capacity;
	}

	private int compareLexicographicIds(int leftMask, int rightMask, List<OrderDto> orders) {
		List<String> leftIds = idsForMask(leftMask, orders);
		List<String> rightIds = idsForMask(rightMask, orders);
		int commonSize = Math.min(leftIds.size(), rightIds.size());
		for (int idx = 0; idx < commonSize; idx++) {
			int comparison = leftIds.get(idx).compareTo(rightIds.get(idx));
			if (comparison != 0) {
				return comparison;
			}
		}
		return Integer.compare(leftIds.size(), rightIds.size());
	}

	private List<String> idsForMask(int mask, List<OrderDto> orders) {
		List<String> ids = new ArrayList<>();
		for (int idx = 0; idx < orders.size(); idx++) {
			if ((mask & (1 << idx)) != 0) {
				ids.add(orders.get(idx).id());
			}
		}
		return ids;
	}
}
