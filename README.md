# SmartLoad Optimization API

Stateless REST API that selects the optimal combination of compatible orders for a truck while maximizing `payout_cents`.

## Stack

- Java 21
- Spring Boot 4
- Maven Wrapper
- Docker / Docker Compose

## Key Design Choices

- Stateless service (no database).
- In-memory optimization only.
- Money handled in integer cents (`long`) only.
- Deterministic output for tie cases.

## Compatibility and Constraints

The selected subset must satisfy all of the following:

1. Capacity constraints:
   - `sum(weight_lbs) <= truck.max_weight_lbs`
   - `sum(volume_cuft) <= truck.max_volume_cuft`
2. Lane compatibility:
   - all selected orders share the same normalized `origin` and `destination`
3. Hazmat isolation:
   - selected orders cannot mix hazmat and non-hazmat (`is_hazmat` must be uniform)
4. Time-window compatibility:
   - per-order validation: `pickup_date <= delivery_date`
   - subset feasibility rule: `max(pickup_date) <= min(delivery_date)`

## Input Validation

- `orders` max size is 22; requests above this return `413 Payload Too Large`
- duplicate order IDs return `400 Bad Request`
- malformed payload / invalid field types return `400 Bad Request`
- invalid date ordering returns `400 Bad Request`

## Optimization Logic

Uses bitmask dynamic programming / subset enumeration for `n <= 22`.

- Time complexity: approximately `O(2^n)` for the main pass plus deterministic tie-break comparisons.
- Space complexity: `O(2^n)` for payout/weight/volume/compatibility arrays.

Tie-break order when payout is equal:

1. Higher weight utilization
2. Higher volume utilization
3. Lexicographically smaller selected order IDs

### Optional configurable objective weights

You can optionally pass `objective_weights` to bias selection toward utilization:

```json
"objective_weights": {
  "payout": 0.2,
  "weight_utilization": 0.4,
  "volume_utilization": 0.4
}
```

Rules:

- each weight must be `>= 0`
- sum of all three must be `> 0`
- if omitted, default behavior is payout-first optimization (original logic)

## API

### `POST /api/v1/load-optimizer/optimize`

Request body:

```json
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
    }
  ],
  "objective_weights": {
    "payout": 0.6,
    "weight_utilization": 0.2,
    "volume_utilization": 0.2
  }
}
```

Success response:

```json
{
  "truck_id": "truck-123",
  "selected_order_ids": ["ord-001", "ord-002"],
  "total_payout_cents": 430000,
  "total_weight_lbs": 30000,
  "total_volume_cuft": 2100,
  "utilization_weight_percent": 68.18,
  "utilization_volume_percent": 70.0
}
```

## HTTP Status Codes

- `200 OK`: valid optimization result
- `400 Bad Request`: invalid payload / validation failure
- `413 Payload Too Large`: more than 22 orders
- `500 Internal Server Error`: unexpected internal error

## Run

### Docker (recommended)

```bash
git clone https://github.com/aman5600/smartload-optimizer.git
cd smartload-optimizer
docker compose up --build
```

### Local

```bash
./mvnw spring-boot:run
```

Service:

- `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`

## Quick Verify

```bash
curl -sS http://localhost:8080/actuator/health
curl -sS -X POST http://localhost:8080/api/v1/load-optimizer/optimize \
  -H "Content-Type: application/json" \
  --data-binary @sample-request.json
```

## Tests

```bash
./mvnw test
```
