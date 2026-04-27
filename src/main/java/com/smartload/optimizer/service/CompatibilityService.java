package com.smartload.optimizer.service;

import com.smartload.optimizer.dto.OrderDto;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class CompatibilityService {

	public boolean sameLane(OrderDto left, OrderDto right) {
		return normalizeLocation(left.origin()).equals(normalizeLocation(right.origin()))
				&& normalizeLocation(left.destination()).equals(normalizeLocation(right.destination()));
	}

	public boolean hazmatCompatible(OrderDto left, OrderDto right) {
		return left.isHazmat() == right.isHazmat();
	}

	public static String normalizeLocation(String location) {
		return location.trim().toLowerCase(Locale.ROOT);
	}
}
