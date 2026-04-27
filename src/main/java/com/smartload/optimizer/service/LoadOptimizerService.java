package com.smartload.optimizer.service;

import com.smartload.optimizer.dto.OptimizeLoadRequest;
import com.smartload.optimizer.dto.OptimizeLoadResponse;
import com.smartload.optimizer.dto.OrderDto;
import com.smartload.optimizer.dto.TruckDto;
import com.smartload.optimizer.model.CandidateSolution;
import com.smartload.optimizer.validation.RequestValidator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LoadOptimizerService {
	private final RequestValidator requestValidator;
	private final OptimizationEngine optimizationEngine;

	public LoadOptimizerService(RequestValidator requestValidator, OptimizationEngine optimizationEngine) {
		this.requestValidator = requestValidator;
		this.optimizationEngine = optimizationEngine;
	}

	public OptimizeLoadResponse optimize(OptimizeLoadRequest request) {
		requestValidator.validate(request);

		TruckDto truck = request.truck();
		List<OrderDto> orders = request.orders();
		CandidateSolution candidate = optimizationEngine.optimize(truck, orders);

		List<String> selectedOrderIds = candidate.selectedIndexes()
				.stream()
				.map(index -> orders.get(index).id())
				.toList();

		return new OptimizeLoadResponse(
				truck.id(),
				selectedOrderIds,
				candidate.totalPayoutCents(),
				candidate.totalWeightLbs(),
				candidate.totalVolumeCuft(),
				utilizationPercentage(candidate.totalWeightLbs(), truck.maxWeightLbs()),
				utilizationPercentage(candidate.totalVolumeCuft(), truck.maxVolumeCuft())
		);
	}

	private double utilizationPercentage(long used, long capacity) {
		if (capacity <= 0) {
			return 0.0;
		}
		return BigDecimal.valueOf(used)
				.multiply(BigDecimal.valueOf(100))
				.divide(BigDecimal.valueOf(capacity), 2, RoundingMode.HALF_UP)
				.doubleValue();
	}
}
