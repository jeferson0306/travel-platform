# 0018 — Engineering assistant: local Ollama, "stuff everything" RAG over docs/context

- Status: Accepted
- Date: 2026-07-23

## Context

ROADMAP's M19 (Phase 5) calls for "Engineering assistants: docs assistant,
code assistant, architecture assistant, deployed as their own service(s)."
Scoped down, with the user's explicit agreement, to one real service - a
single `assistant-service` answering questions about this platform,
grounded in the `docs/context`/`docs/prompts` corpus M18 (ADR 0017) built -
rather than three separate assistant services with overlapping concerns.

Building any real assistant needs an LLM. A hosted API (Anthropic, OpenAI)
was the first option discussed, but neither has a permanently free tier,
and the user does not want to pay for this milestone. The alternative,
chosen with the user: **Ollama, running locally** - free, and consistent
with this platform's existing local-first posture (LocalStack instead of
real AWS, `kind` instead of a real cluster, Toxiproxy instead of a real
chaos engineering vendor). `llama3:latest` (already present on the
development machine) was used as the default model - a real, currently-
pullable Ollama tag, not a guess (M16's earlier Toxiproxy image-tag lesson
directly informed double-checking this).

## Decision

### One new service, hexagonal like every other backend service

`assistant-service` (port 8088) follows the same
`api/application/domain/infrastructure` layout and ArchUnit-enforced
boundaries as every other service (docs/context/conventions.md). No
MongoDB, no Kafka - it's stateless and read-only; its one synchronous
external dependency is Ollama, the third such dependency in this platform
after gateway->backends and search-service->OpenSearch (ADR 0014), so it
gets the same `@Timeout`/`@Retry` treatment on its one call.

### "Stuff everything" RAG, not chunking + embeddings + vector search

The whole `docs/context`/`docs/prompts` corpus (11 Markdown files, ~26KB,
~7,000 tokens) is bundled into the running service at build time (Maven
resource copy from the repo-root `docs/` directories into this service's
own `src/main/resources`, so the Docker build context stays
`backend/assistant-service` like every other service) and included **in
full** on every request as the system prompt. No retrieval step, no
embeddings, no vector database - a corpus this small doesn't need one, and
skipping that machinery means there's no ranking/retrieval step to get
wrong. Genuinely wouldn't scale past a few hundred KB of context, but
that's not this corpus.

The exact list of bundled files is a hardcoded constant
(`ClasspathContextCorpusRepository.RESOURCE_NAMES`), not discovered by
scanning the classpath at runtime - a plain JAR doesn't expose directory
listings without an extra dependency, and an explicit list is honest about
the one real cost of this approach (a new `docs/context`/`docs/prompts`
file needs adding here too) rather than hiding it behind scanning
machinery. Same procedural-accuracy trade-off ADR 0017 already made for
keeping the corpus itself current.

### Internal tool, gated the same way as other internal reads

`POST /api/v1/assistant/ask` requires MANAGER/ADMIN/SUPER_ADMIN/SUPPORT
(docs/adr/0006-rbac-roles.md) - the same non-USER gate
payment-service/notification-service already use for their read endpoints,
because a plain traveler account has no reason to query it, not because
the answers are sensitive.

## Findings (from actually running this against the real local Ollama)

Two real problems surfaced only by testing with a genuine question and a
genuine model, not by reasoning about the design on paper:

1. **The first real test hallucinated completely**: asked "What roles
   exist in this platform and which service issues JWTs?", the model
   answered about GitHub - a platform never mentioned anywhere in the
   corpus. Root cause: Ollama's default `num_ctx` (context window) is
   2048 tokens, well under this corpus's ~7,000 tokens - the model
   silently truncated most of the system prompt (including the actual
   grounding material) and confabulated instead of saying it didn't know.
   Fixed by explicitly setting `options.num_ctx` on every chat request
   (`assistant.ollama.num-ctx`, default 8192, matching `llama3:latest`'s
   own trained context length) - not a tuning knob turned up "to be safe,"
   the number that made the very failure above stop happening.
2. **Re-running the same question after the fix produced a correct,
   cited answer** ("USER, MANAGER, ADMIN, SUPER_ADMIN, SUPPORT... it is
   the `identity-service` (port 8081) that issues RS256-signed JWT..."),
   and a second question (which Kafka topics `search-service` consumes,
   and its consumer group) also came back correct, quoting the exact
   table row from `event-catalog.md`. First response after a cold model
   load took ~28s; the second, with the model already resident in Ollama,
   took ~5s - both against `llama3:latest` (an 8B, Q4-quantized model) on
   unaccelerated local hardware, not a number to extrapolate a production
   SLA from.

## Consequences

- A genuinely working, locally-verified engineering assistant exists, at
  zero API cost - but its quality is tied to whatever model is pulled
  locally; a smaller/faster model was not evaluated for this milestone,
  and `llama3:latest` (~4.7GB) is a real, non-trivial download/resource
  cost the docker-compose/Kubernetes wiring both inherit.
- **Kubernetes verification for this milestone is manifests-only, not
  live-deployed** - unlike M17 (ADR 0016), where every manifest was
  actually run on a local `kind` cluster. `infrastructure/kubernetes/base/infra/ollama.yaml`
  and `apps/assistant-service.yaml` were written following the same
  established patterns (headless-Service lessons from Kafka don't apply
  here - Ollama has no self-referencing quorum) and validated with
  `kubectl kustomize`, but adding a multi-GB model on top of the already
  resource-constrained local cluster from ADR 0016 was not attempted this
  round. docker-compose (`make apps-up`, now including `ollama` +
  `ollama-init` + `assistant-service`) is this milestone's actual verified
  deployment target; a future milestone extending the K8s deployment
  should budget real time for the same kind of resource tuning M17 needed.
- `gateway` gained an eighth upstream segment (`assistant` ->
  `assistant-service:8088`, `RoutingTable`/`application.yml`) - no other
  gateway behavior changed; the existing rate-limit/JWT-fast-fail/circuit-
  breaker machinery applies to it automatically, same as every other
  segment.
