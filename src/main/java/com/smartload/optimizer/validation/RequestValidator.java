package com.smartload.optimizer.validation;

import com.smartload.optimizer.dto.OptimizeLoadRequest;
import com.smartload.optimizer.dto.OrderDto;
import com.smartload.optimizer.dto.ObjectiveWeightsDto;
import com.smartload.optimizer.exception.BadRequestException;
import com.smartload.optimizer.exception.PayloadTooLargeException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RequestValidator {
	private static final int MAX_ORDERS = 22;

	public void validate(OptimizeLoadRequest request) {
		List<OrderDto> orders = request.orders();
		if (orders.size() > MAX_ORDERS) {
			throw new PayloadTooLargeException("At most %d orders are supported per request".formatted(MAX_ORDERS));
		}

		Set<String> ids = new HashSet<>();
		for (OrderDto order : orders) {
			if (!ids.add(order.id())) {
				throw new BadRequestException("Duplicate order id: " + order.id());
			}
			if (order.pickupDate().isAfter(order.deliveryDate())) {
				throw new BadRequestException("Order %s has pickup_date after delivery_date".formatted(order.id()));
			}
		}

		validateObjectiveWeights(request.objectiveWeights());
	}

	private void validateObjectiveWeights(ObjectiveWeightsDto objectiveWeights) {
		if (objectiveWeights == null) {
			return;
		}
		if (objectiveWeights.sum() <= 0.0) {
			throw new BadRequestException("objective_weights sum must be greater than 0");
		}
	}
}
