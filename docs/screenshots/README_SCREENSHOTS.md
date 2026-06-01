# Screenshot Folder for GitHub

Copy this whole folder into your repository root so the images land under:

```text
docs/screenshots/
```

## Best screenshots to use in README

Use these clean names in the README:

| Feature | Screenshot path |
|---|---|
| Swagger / OpenAPI | `docs/screenshots/swagger-api.png` |
| Actuator health check | `docs/screenshots/actuator-health-up.png` |
| GitHub Actions CI | `docs/screenshots/github-actions-success.png` |
| Testcontainers integration test | `docs/screenshots/testcontainers-build-success.png` |
| Idempotency first request | `docs/screenshots/idempotency-first-order.png` |
| Idempotency duplicate retry | `docs/screenshots/idempotency-duplicate-order.png` |
| Redis cache key | `docs/screenshots/redis-cache-key.png` |
| Kafka order.created event | `docs/screenshots/kafka-order-created-event.png` |
| Transactional outbox event | `docs/screenshots/outbox-event-published.png` |
| All Docker services running | `docs/screenshots/docker-all-services-running.png` |
| Prometheus target UP | `docs/screenshots/prometheus-target-up.png` |
| Grafana dashboard | `docs/screenshots/grafana-dashboard.png` |

## README markdown snippet

```md
## Production-Readiness Proof

### Swagger / OpenAPI
![Swagger API](docs/screenshots/swagger-api.png)

### CI/CD with GitHub Actions
![GitHub Actions Success](docs/screenshots/github-actions-success.png)

### Testcontainers Integration Test
![Testcontainers Build Success](docs/screenshots/testcontainers-build-success.png)

### Idempotency Key - Duplicate Order Prevention
First request creates the order:

![Idempotency First Order](docs/screenshots/idempotency-first-order.png)

Retrying the same request with the same `Idempotency-Key` returns the same order instead of creating a duplicate:

![Idempotency Duplicate Order](docs/screenshots/idempotency-duplicate-order.png)

### Redis Product Cache
![Redis Cache Key](docs/screenshots/redis-cache-key.png)

### Kafka Event Publishing
![Kafka Order Created Event](docs/screenshots/kafka-order-created-event.png)

### Transactional Outbox Pattern
![Outbox Event Published](docs/screenshots/outbox-event-published.png)

### Observability with Prometheus and Grafana
Prometheus target is up:

![Prometheus Target Up](docs/screenshots/prometheus-target-up.png)

Grafana dashboard:

![Grafana Dashboard](docs/screenshots/grafana-dashboard.png)
```

## Full screenshot list

The numbered files are all extracted screenshots, preserved in chronological order:

- `01-actuator-health-up.png`
- `02-swagger-openapi-actuator.png`
- `03-testcontainers-local-build-success.png`
- `04-github-actions-ci-in-progress.png`
- `05-github-actions-ci-success-swagger-actuator.png`
- `06-github-actions-testcontainers-success.png`
- `07-github-actions-testcontainers-workflow-list.png`
- `08-postman-insufficient-stock-validation.png`
- `09-idempotency-first-order-created.png`
- `10-docker-postgres-redis-running.png`
- `11-idempotency-duplicate-order-returned.png`
- `12-redis-db-hit-first-request-log.png`
- `13-redis-cache-key-productbyid.png`
- `14-docker-postgres-redis-kafka-running.png`
- `15-kafka-order-created-postman.png`
- `16-kafka-published-event-terminal-log.png`
- `17-kafka-console-consumer-order-created.png`
- `18-github-actions-kafka-success.png`
- `19-outbox-published-event-terminal-log.png`
- `20-outbox-event-published-db-query.png`
- `21-github-actions-outbox-in-progress.png`
- `22-github-actions-outbox-success.png`
- `23-docker-all-services-running.png`
- `24-prometheus-target-up.png`
- `25-grafana-login-screen.png`
- `26-grafana-login-admin.png`
- `27-grafana-connections-page.png`
- `28-grafana-add-prometheus-data-source.png`
- `29-grafana-prometheus-data-source-settings.png`
- `30-grafana-prometheus-url-save-test.png`
- `31-grafana-prometheus-save-test-success.png`
- `32-grafana-new-dashboard-start.png`
- `33-grafana-new-panel-add.png`
- `34-grafana-panel-configure-menu.png`
- `35-grafana-code-query-mode.png`
- `36-grafana-http-request-rate-query.png`
- `37-grafana-http-request-rate-panel.png`
- `38-grafana-jvm-memory-panel.png`
- `39-grafana-dashboard-final.png`
- `40-prometheus-target-up-final.png`
