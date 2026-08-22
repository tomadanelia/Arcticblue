# Redemption–Subscription Netting Engine

Spring Boot REST service that solves the 0/1-knapsack trade-selection problem: maximize expected P&L without exceeding the desk's margin limit. Each request, every candidate trade, and the selected result are persisted for audit.

## Prerequisites

- Java 21
- Docker with Docker Compose (for PostgreSQL)

## Database setup

The checked-in credentials are deliberately placeholders. Either use them locally as-is or replace them through environment variables.

Copy `.env.example` to `.env` if you want Docker Compose to load your chosen PostgreSQL values automatically. Export the matching `DB_*` values when running the JAR.

```bash
docker compose up -d
```

The Compose defaults are:

```text
database: arcticblue
username: your_postgres_username
password: your_postgres_password
port:     5432
```

To use different values, set `POSTGRES_DB`, `POSTGRES_USER`, and `POSTGRES_PASSWORD` before starting Compose, then provide matching application variables:

```text
DB_URL=jdbc:postgresql://localhost:5432/arcticblue
DB_USERNAME=your_postgres_username
DB_PASSWORD=your_postgres_password
```

Flyway applies the versioned schema automatically at startup.

## Build and run

On Windows:

```powershell
.\mvnw.cmd clean verify
$env:DB_USERNAME="your_postgres_username"
$env:DB_PASSWORD="your_postgres_password"
java -jar target/arcticblue-0.0.1-SNAPSHOT.jar
```

On macOS/Linux:

```bash
./mvnw clean verify
DB_USERNAME=your_postgres_username DB_PASSWORD=your_postgres_password \
  java -jar target/arcticblue-0.0.1-SNAPSHOT.jar
```

The API listens on `http://localhost:8080`.

## API examples

### Optimize trades

```bash
curl -i -X POST http://localhost:8080/api/v1/trades/optimize \
  -H "Content-Type: application/json" \
  -d '{
    "maxMargin": 15,
    "candidateTrades": [
      {"tradeName":"Trade Alpha","marginRequired":5,"expectedPnl":120},
      {"tradeName":"Trade Beta","marginRequired":10,"expectedPnl":200},
      {"tradeName":"Trade Gamma","marginRequired":3,"expectedPnl":80},
      {"tradeName":"Trade Delta","marginRequired":8,"expectedPnl":160}
    ]
  }'
```

Returns HTTP `201 Created`:

```json
{
  "requestId": "c3d4e5f6-a7b8-4abc-9123-123456789abc",
  "selectedTrades": [
    {"tradeName":"Trade Alpha","marginRequired":5,"expectedPnl":120},
    {"tradeName":"Trade Beta","marginRequired":10,"expectedPnl":200}
  ],
  "totalMarginRequired": 15,
  "totalExpectedPnl": 320,
  "createdAt": "2025-06-01T10:00:00Z"
}
```

### Retrieve one persisted run

```bash
curl http://localhost:8080/api/v1/trades/c3d4e5f6-a7b8-4abc-9123-123456789abc
```

Returns HTTP `200` with the same response shape as optimization, or HTTP `404` when the ID is unknown.

### Retrieve the audit trail

```bash
curl "http://localhost:8080/api/v1/trades?page=0&size=20"
```

Returns HTTP `200`:

```json
{
  "content": [{
    "requestId":"c3d4e5f6-a7b8-4abc-9123-123456789abc",
    "selectedTrades":[],
    "totalMarginRequired":0,
    "totalExpectedPnl":0,
    "createdAt":"2025-06-01T10:00:00Z"
  }],
  "page":0,
  "size":20,
  "totalElements":1,
  "totalPages":1
}
```

Results are newest first. `page` starts at zero; `size` must be between 1 and 100.

## Validation and optimization behavior

`maxMargin` must be zero or greater. Candidate margins must be positive, names must be nonblank (maximum 200 characters), and all fields must be present. Expected P&L may be negative; loss-making trades are naturally excluded. Currency values are whole units represented as signed 64-bit integers. A request may contain at most 1,000 candidates.

The algorithm keeps only non-dominated `(margin, P&L)` states after each candidate, providing an exact 0/1-knapsack result without allocating memory proportional to a potentially large margin limit. Ties in total P&L are resolved by choosing the lower-margin result, then stable input order.

Invalid payloads return HTTP `400` with a message and field-level validation errors. If nothing fits (including an empty candidate list), the API returns HTTP `201` for POST with an empty selection and zero totals. The assignment's HTTP `200` no-fit rule applies to the result itself; the endpoint-specific requirement mandates HTTP `201` for every successful POST.

## Schema and indexes

- `optimization_runs` stores the UUID request ID, input margin limit, result totals, and UTC creation timestamp.
- `optimization_trades` stores every input candidate in original order and a `selected` flag. It has a foreign key with cascade delete to its run.
- `idx_optimization_runs_created_at` supports newest-first audit pagination.
- `idx_optimization_trades_request_selected` supports reconstruction of selected trades for a run.
- A unique `(request_id, candidate_order)` constraint preserves an unambiguous input ordering.

## Tests

```bash
./mvnw test
```

Unit tests cover the optimizer, including no-fit, negative-P&L, and tie cases. Integration tests start the full Spring context against an in-memory PostgreSQL-compatible H2 database, apply Flyway migrations, and exercise POST, GET-by-ID, pagination, validation, persistence, and 404 behavior through MockMvc.
