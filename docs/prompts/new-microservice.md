# Prompt: scaffold a new microservice

Use this template for a ninth-plus service. The closest reference
implementation for each concern is named inline - copy patterns from the
code, don't reinvent.

---

Scaffold `[service]-service` (port `[80XX]` - next free in the 8081-8087
sequence) in `backend/[service]-service`, matching the platform
conventions (docs/context/conventions.md, docs/context/service-catalog.md):

- Quarkus 3 / Java 25 module added to `backend/pom.xml`; hexagonal
  packages `api/application/domain/infrastructure` with the ArchUnit test
  enforcing them (copy from notification-service - the smallest).
- Datastore: [MongoDB database `[service]` | OpenSearch | none - justify].
- Security: RS256 JWT verification with identity-service's public key
  (copy flight-service's setup); `@RolesAllowed` per endpoint.
- Observability: shared structured-JSON request logging, `/health/ready`,
  `/health/live`, Prometheus metrics (M6 baseline - copy any service).
- Events: outbox pattern if it publishes (ADR 0007); the standard consumer
  shape if it consumes (docs/prompts/new-kafka-consumer.md).
- Canonical error shape via exception mappers.
- Wiring, all mandatory in the same PR:
  - `ci.yml` service matrix,
  - docker-compose `apps` profile (container_name = `[service]-service`,
    healthcheck, MONGO_URI/KAFKA_BOOTSTRAP_SERVERS as applicable),
  - `infrastructure/kubernetes/base/apps/[service]-service.yaml`
    (Deployment+Service+HPA, `enableServiceLinks: false`, probes on
    /health/ready|live - copy an existing app manifest) + kustomization
    entry + local-overlay resource trim,
  - gateway upstream (`%prod` URL + GATEWAY*UPSTREAM*[SEGMENT]) if it has
    an API,
  - Kafka topics in `create-topics.sh` + the k8s ConfigMap copy,
  - docs: ADR, docs/events file if it has events, and updates to
    docs/context/platform-overview.md, service-catalog.md,
    event-catalog.md, ROADMAP.md, CHANGELOG.md, ARCHITECTURE.md.
- Tests: unit + integration (Testcontainers/Dev Services) + ArchUnit,
  JaCoCo floor 0.45, all `@DisplayName`d.
