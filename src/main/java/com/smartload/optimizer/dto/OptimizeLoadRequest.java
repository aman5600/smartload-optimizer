package com.smartload.optimizer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OptimizeLoadRequest(
		@NotNull @Valid TruckDto truck,
		@NotNull @Valid List<OrderDto> orders
) {
}
