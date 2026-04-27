package com.smartload.optimizer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smartload.optimizer.dto.OptimizeLoadRequest;
import com.smartload.optimizer.dto.OptimizeLoadResponse;
import com.smartload.optimizer.dto.OrderDto;
import com.smartload.optimizer.dto.TruckDto;
import com.smartload.optimizer.exception.BadRequestException;
import com.smartload.optimizer.validation.RequestValidator;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoadOptimizerServiceTest {
	private LoadOptimizerService service;

	@BeforeEach
	void setUp() {
		CompatibilityService compatibilityService = new CompatibilityService();
		OptimizationEngine optimizationEngine = new OptimizationEngine(compatibilityService);
		service = new LoadOptimizerService(new RequestValidator(), optimizationEngine);
	}

	@Test
	void shouldSelectOptimalOrdersFromPromptExample() {
		TruckDto truck = new TruckDto("truck-123", 44_000, 3_000);
		List<OrderDto> orders = List.of(
				order("ord-001", 250_000, 18_000, 1_200, "Los Angeles, CA", "Dallas, TX",
						"2025-12-05", "2025-12-09", false),
				order("ord-002", 180_000, 12_000, 900, "Los Angeles, CA", "Dallas, TX",
						"2025-12-04", "2025-12-10", false),
				order("ord-003", 320_000, 30_000, 1_800, "Los Angeles, CA", "Dallas, TX",
						"2025-12-06", "2025-12-08", true)
		);

		OptimizeLoadResponse result = service.optimize(new OptimizeLoadRequest(truck, orders));

		assertEquals(List.of("ord-001", "ord-002"), result.selectedOrderIds());
		assertEquals(430_000, result.totalPayoutCents());
		assertEquals(30_000, result.totalWeightLbs());
		assertEquals(2_100, result.totalVolumeCuft());
		assertEquals(68.18, result.utilizationWeightPercent());
		assertEquals(70.0, result.utilizationVolumePercent());
	}

	@Test
	void shouldRespectHazmatIsolation() {
		TruckDto truck = new TruckDto("truck-999", 44_000, 4_000);
		List<OrderDto> orders = List.of(
				order("ord-non-1", 100_000, 10_000, 500, "A", "B", "2025-12-01", "2025-12-04", false),
				order("ord-haz-1", 110_000, 10_000, 500, "A", "B", "2025-12-01", "2025-12-04", true),
				order("ord-haz-2", 120_000, 10_000, 500, "A", "B", "2025-12-01", "2025-12-04", true)
		);

		OptimizeLoadResponse result = service.optimize(new OptimizeLoadRequest(truck, orders));

		assertEquals(List.of("ord-haz-1", "ord-haz-2"), result.selectedOrderIds());
		assertEquals(230_000, result.totalPayoutCents());
	}

	@Test
	void shouldReturnEmptySelectionWhenNoOrderFitsCapacity() {
		TruckDto truck = new TruckDto("truck-456", 5_000, 300);
		List<OrderDto> orders = List.of(
				order("ord-001", 100_000, 6_000, 400, "A", "B", "2025-12-01", "2025-12-02", false)
		);

		OptimizeLoadResponse result = service.optimize(new OptimizeLoadRequest(truck, orders));

		assertEquals(List.of(), result.selectedOrderIds());
		assertEquals(0, result.totalPayoutCents());
	}

	@Test
	void shouldRejectOrderWithPickupAfterDelivery() {
		TruckDto truck = new TruckDto("truck-123", 44_000, 3_000);
		List<OrderDto> orders = List.of(
				order("ord-001", 100_000, 1_000, 100, "A", "B", "2025-12-06", "2025-12-01", false)
		);

		assertThrows(BadRequestException.class, () -> service.optimize(new OptimizeLoadRequest(truck, orders)));
	}

	private OrderDto order(
			String id,
			long payout,
			long weight,
			long volume,
			String origin,
			String destination,
			String pickupDate,
			String deliveryDate,
			boolean isHazmat
	) {
		return new OrderDto(
				id,
				payout,
				weight,
				volume,
				origin,
				destination,
				LocalDate.parse(pickupDate),
				LocalDate.parse(deliveryDate),
				isHazmat
		);
	}
}
