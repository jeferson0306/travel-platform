# 0002 — Use Quarkus as the backend framework

- Status: Accepted
- Date: 2026-07-22

## Context

The backend needs a Java framework for a set of independently deployable
microservices. Candidates considered: Spring Boot, Micronaut, Quarkus, and
plain Java with a lightweight HTTP layer (e.g. Javalin).

Requirements driving the choice:

- First-class support for modern Java (virtual threads, records, sealed
  classes, pattern matching) without fighting the framework.
- Fast startup and low memory footprint, relevant for running many services
  locally at once on a single developer machine, and for realistic
  container resource limits later.
- Native support for the cross-cutting concerns this project needs out of
  the box: health checks, metrics, OpenAPI, fault tolerance, reactive
  messaging — without assembling them from many unrelated libraries.
- A testing story that supports Testcontainers-based integration tests
  cleanly.

## Decision

Use **Quarkus 3** for every backend service, with RESTEasy Reactive for HTTP,
Hibernate Validator for bean validation, SmallRye for OpenAPI/Health/Metrics/
Config, MapStruct for DTO mapping, and Quarkus Fault Tolerance for
resilience.

Rejected alternatives:

- **Spring Boot** — the most common choice in Brazilian job postings, and a
  reasonable one, but its startup/memory profile and legacy servlet-first
  model are a worse fit for a project explicitly built around modern Java
  and many small services. Quarkus was chosen deliberately to also
  demonstrate range beyond the most common framework.
- **Micronaut** — comparable design goals to Quarkus, but a smaller ecosystem
  and less mature Kafka/reactive messaging integration at the time of this
  decision.
- **Plain Java + lightweight HTTP** — would maximize control but requires
  reimplementing health checks, metrics, config, and validation, which adds
  risk without adding signal about engineering ability.

## Consequences

- Every service shares the same framework idioms (CDI, `@ApplicationScoped`,
  MicroProfile Config), which keeps the "service template" from
  [ENGINEERING.md](../../ENGINEERING.md) consistent across services.
- Native-image (GraalVM) compilation is available if startup time ever
  becomes a demonstrated concern, without a rewrite.
- Contributors who only know Spring will have a small ramp-up cost — accepted
  because this is a single-maintainer project where that cost is paid once.
