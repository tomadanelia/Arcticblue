# Redemption–Subscription Netting Engine

Spring Boot REST service that solves the 0/1-knapsack trade-selection problem: maximize expected P&L without exceeding the desk's margin limit. Each request, every candidate trade, and the selected result are persisted for audit.

## Prerequisites

- Java 21
- Docker with Docker Compose (for PostgreSQL)

## Database Setup

The application requires environment variables to configure PostgreSQL and Spring Boot. **There are no default fallback values configured in `application.properties` or `docker-compose.yml`.**

1. Create a `.env` file from `.env.example`:

```bash
cp .env.example .env
```

*(On Windows PowerShell: `Copy-Item .env.example .env`)*

2. Start the PostgreSQL database using Docker Compose (Docker Compose automatically loads values from `.env`):

```bash
docker compose up -d
```

The default values inside `.env.example` set up the following configuration:

```text
POSTGRES_DB=arcticblue
POSTGRES_USER=your_postgres_username
POSTGRES_PASSWORD=your_postgres_password

DB_URL=jdbc:postgresql://localhost:5432/arcticblue
DB_USERNAME=your_postgres_username
DB_PASSWORD=your_postgres_password
```

Flyway applies the versioned schema automatically when the application starts up.

## Build and Run

Because Spring Boot does not automatically read `.env` files when launched directly as a standalone JAR, you must explicitly export/set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in your shell terminal before running the application.

### On Windows (PowerShell)

```powershell
# Build and run tests
.\mvnw.cmd clean verify

# Set required environment variables
$env:DB_URL="jdbc:postgresql://localhost:5432/arcticblue"
$env:DB_USERNAME="your_postgres_username"
$env:DB_PASSWORD="your_postgres_password"

# Launch the application
java -jar target/arcticblue-0.0.1-SNAPSHOT.jar
```

### On macOS / Linux (Bash or Zsh)

```bash
# Build and run tests
./mvnw clean verify

# Launch the application with required environment variables
DB_URL="jdbc:postgresql://localhost:5432/arcticblue" \
DB_USERNAME="your_postgres_username" \
DB_PASSWORD="your_postgres_password" \
java -jar target/arcticblue-0.0.1-SNAPSHOT.jar
```

The API listens on `http://localhost:8080`.

## API Examples

### 1. Optimize Trades (`POST /api/v1/trades/optimize`)

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

### 2. Retrieve One Persisted Run (`GET /api/v1/trades/{requestId}`)

```bash
curl http://localhost:8080/api/v1/trades/c3d4e5f6-a7b8-4abc-9123-123456789abc
```

Returns HTTP `200 OK` with the persisted optimization result, or HTTP `404 Not Found` if the `requestId` does not exist.

### 3. Retrieve the Audit Trail (`GET /api/v1/trades`)

```bash
curl "http://localhost:8080/api/v1/trades?page=0&size=20"
```

Returns HTTP `200 OK`:

```json
{
  "content": [
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
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

Results are ordered newest first. `page` starts at 0, and `size` must be between 1 and 100.

## Validation and Optimization Behavior

- `maxMargin` must be zero or greater.
- Candidate margins must be positive.
- Trade names must be nonblank and no longer than 200 characters.
- All fields must be present.
- Expected P&L may be negative; loss-making trades are naturally excluded.
- Currency values are whole units represented as signed 64-bit integers.
- A request may contain at most 1,000 candidates.

The algorithm keeps only non-dominated `(margin, P&L)` states after each candidate, providing an exact 0/1-knapsack result without allocating memory proportional to a potentially large margin limit. Ties in total P&L are resolved by choosing the lower-margin result, then stable input order.

Invalid payloads return HTTP `400` with a message and field-level validation errors.

A successful optimization that selects at least one trade returns HTTP `201`.

If no trade is selected (including when no candidate fits or the candidate list is empty), `POST` returns HTTP `200` with an empty selection and zero totals, as required by the no-fit rule.

## Schema and Indexes

### `optimization_runs`

Stores:

- UUID request ID
- Input margin limit
- Result totals
- UTC creation timestamp

### `optimization_trades`

Stores:

- Every input candidate in original order
- `selected` flag

Database constraints and indexes:

- Foreign key with cascade delete to its run
- `idx_optimization_runs_created_at` supports newest-first audit pagination
- `idx_optimization_trades_request_selected` supports reconstruction of selected trades for a run
- Unique `(request_id, candidate_order)` constraint preserves unambiguous input ordering

## Tests

```bash
./mvnw test
```

Unit tests cover the optimizer, including:

- No-fit cases
- Negative P&L cases
- Tie-breaking behavior

Integration tests:

- Start the full Spring context against an in-memory PostgreSQL-compatible H2 database
- Apply Flyway migrations
- Exercise POST endpoints
- Exercise GET-by-ID endpoints
- Exercise pagination
- Validate request validation behavior
- Verify persistence
- Verify `404 Not Found` behavior through MockMvc