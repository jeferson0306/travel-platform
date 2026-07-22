# 0008 — Use Terraform + LocalStack for AWS resources

- Status: Accepted
- Date: 2026-07-22

## Context

Kafka covers the platform's cross-service event backbone (ADR 0004), but
some needs are naturally AWS-shaped rather than message-shaped: durable
object storage for generated artifacts (booking receipts, eventually
invoices/exports), and later transactional email (SES), application
config/secrets (Secrets Manager/SSM), and point-to-point async work (SQS).
Provisioning these by hand (clicking through a console, or an
undocumented CLI script) is exactly the kind of drift this project's
existing infrastructure-as-code discipline (Docker Compose, CI) rejects
elsewhere, and it would make local development depend on a real AWS
account and its cost, which does not fit a project meant to run entirely
with `docker compose up`.

## Decision

Provision every AWS resource the platform depends on with **Terraform**,
against **LocalStack** for local development, using the same modules a
real environment would use.

- [`infrastructure/terraform/modules`](../../infrastructure/terraform/modules) -
  reusable modules per AWS resource type (starting with `s3`).
- [`infrastructure/terraform/environments/local`](../../infrastructure/terraform/environments/local) -
  the root module for local dev, pointed at LocalStack
  (`http://localhost:4566`) via the AWS provider's `endpoints` block and the
  well-known LocalStack fake credential pair (`test`/`test` - not a real
  secret, see [SECURITY.md](../../SECURITY.md)). A real environment gets its
  own `environments/<env>` directory reusing the same modules against the
  real AWS provider (no `endpoints` override, real credentials from the
  environment/IAM role).
- LocalStack itself already runs as part of `docker compose up`
  ([infrastructure/docker/docker-compose.yml](../../infrastructure/docker/docker-compose.yml))
  with `SERVICES=s3,sqs,sns,ses,secretsmanager,ssm,eventbridge` - Terraform
  provisions resources inside it, it does not start it.

First resource: an S3 bucket (`booking-receipts`) that `booking-service`
writes a JSON booking confirmation to on creation - see
[docs/adr/0009-booking-receipts-in-s3.md](0009-booking-receipts-in-s3.md).

Rejected alternatives:

- **Manual provisioning (console/CLI script)** - not reproducible, not
  reviewable in a PR diff, and impossible to run identically in CI/local
  dev without a real AWS account.
- **LocalStack's own resource-init scripts (`docker-entrypoint-initaws.d`)** -
  simpler for a single bucket, but does not scale past a handful of
  resources and gives up the plan/diff review Terraform provides; also
  would not transfer to a real environment without being rewritten anyway.
- **AWS CDK / Pulumi** - equally valid, but Terraform's HCL is
  declarative and provider-agnostic in a way that keeps the LocalStack vs.
  real-AWS difference to a few lines in `providers.tf`, and it is the
  more common choice a reviewer would expect to see.

## Consequences

- Local development needs `terraform` installed (documented in
  [infrastructure/terraform/environments/local/README.md](../../infrastructure/terraform/environments/local/README.md)),
  in addition to Docker.
- Every new AWS resource a service needs is added as a Terraform module +
  an environment entry, in the same PR as the code that uses it - not
  provisioned out-of-band.
- CI does not yet run `terraform plan`/`apply` (no environment beyond
  local exists to apply to); revisit once a real deployment target exists.
