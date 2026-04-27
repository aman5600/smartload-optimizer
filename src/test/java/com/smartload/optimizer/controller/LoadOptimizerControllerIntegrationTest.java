package com.smartload.optimizer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoadOptimizerControllerIntegrationTest {
	@LocalServerPort
	private int port;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Test
	void shouldReturn200ForValidRequest() throws IOException, InterruptedException {
		String payload = """
				{
				  "truck": {
				    "id": "truck-123",
				    "max_weight_lbs": 44000,
				    "max_volume_cuft": 3000
				  },
				  "orders": [
				    {
				      "id": "ord-001",
				      "payout_cents": 250000,
				      "weight_lbs": 18000,
				      "volume_cuft": 1200,
				      "origin": "Los Angeles, CA",
				      "destination": "Dallas, TX",
				      "pickup_date": "2025-12-05",
				      "delivery_date": "2025-12-09",
				      "is_hazmat": false
				    },
				    {
				      "id": "ord-002",
				      "payout_cents": 180000,
				      "weight_lbs": 12000,
				      "volume_cuft": 900,
				      "origin": "Los Angeles, CA",
				      "destination": "Dallas, TX",
				      "pickup_date": "2025-12-04",
				      "delivery_date": "2025-12-10",
				      "is_hazmat": false
				    }
				  ]
				}
				""";

		HttpResponse<String> response = postOptimize(payload);

		assertEquals(200, response.statusCode());
		assertTrue(response.body().contains("\"truck_id\":\"truck-123\""));
		assertTrue(response.body().contains("\"total_payout_cents\":430000"));
	}

	@Test
	void shouldReturn400ForInvalidDateRange() throws IOException, InterruptedException {
		String payload = """
				{
				  "truck": {
				    "id": "truck-123",
				    "max_weight_lbs": 44000,
				    "max_volume_cuft": 3000
				  },
				  "orders": [
				    {
				      "id": "ord-001",
				      "payout_cents": 100000,
				      "weight_lbs": 1000,
				      "volume_cuft": 100,
				      "origin": "A",
				      "destination": "B",
				      "pickup_date": "2025-12-06",
				      "delivery_date": "2025-12-01",
				      "is_hazmat": false
				    }
				  ]
				}
				""";

		HttpResponse<String> response = postOptimize(payload);

		assertEquals(400, response.statusCode());
	}

	@Test
	void shouldReturn413WhenOrderCountExceedsLimit() throws IOException, InterruptedException {
		String payload = payloadWithOrderCount(23);

		HttpResponse<String> response = postOptimize(payload);

		assertEquals(413, response.statusCode());
	}

	private HttpResponse<String> postOptimize(String jsonPayload) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + "/api/v1/load-optimizer/optimize"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
				.build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private String payloadWithOrderCount(int orderCount) {
		StringBuilder builder = new StringBuilder();
		builder.append("""
				{
				  "truck": {
				    "id": "truck-123",
				    "max_weight_lbs": 44000,
				    "max_volume_cuft": 3000
				  },
				  "orders": [
				""");
		for (int idx = 1; idx <= orderCount; idx++) {
			builder.append("""
					{
					  "id": "ord-%03d",
					  "payout_cents": 100000,
					  "weight_lbs": 1000,
					  "volume_cuft": 50,
					  "origin": "A",
					  "destination": "B",
					  "pickup_date": "2025-12-01",
					  "delivery_date": "2025-12-05",
					  "is_hazmat": false
					}
					""".formatted(idx));
			if (idx < orderCount) {
				builder.append(",");
			}
		}
		builder.append("""
				  ]
				}
				""");
		return builder.toString();
	}
}
