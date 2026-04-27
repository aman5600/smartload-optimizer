# SmartLoad Optimization API

Stateless REST API that returns the optimal set of compatible orders for a truck while maximizing payout.

## Tech

- Java 21
- Spring Boot
- Maven Wrapper
- Docker / Docker Compose

## Rules Enforced

- Capacity: total weight and volume must stay within truck limits.
- Route compatibility: all selected orders must share the same normalized `origin` and `destination`.
- Hazmat isolation: selected orders cannot mix hazmat and non-hazmat freight.
- Time-window compatibility: selected set must have a non-empty common feasible interval (`max(pickup_date) <= min(delivery_date)`).
- Money uses integer cents (`long`) only.

## How to run

```bash
git clone <your-repo>
cd smartload-optimizer
docker compose up --build
```

Service runs at:

- `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`

## API

### Optimize load

`POST /api/v1/load-optimizer/optimize`

Example:

```bash
curl -X POST http://localhost:8080/api/v1/load-optimizer/optimize \
  -H "Content-Type: application/json" \
  -d @sample-request.json
```

Sample success response:

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

## HTTP Statuses

- `200` Success
- `400` Invalid input / validation error
- `413` More than 22 orders in request

## Algorithm

Bitmask dynamic programming / subset enumeration for `n <= 22`.

- Complexity: `O(2^n * n)` for tie-break ID comparison in the worst case.
- Space: `O(2^n)` arrays for cumulative payout/weight/volume and compatibility state.
- Deterministic tie-breaks:
  1. Higher payout
  2. Higher weight utilization
  3. Higher volume utilization
  4. Lexicographically smaller selected order IDs

## Test

```bash
./mvnw test
```
