# local environment

Terraform root module wiring the modules in [../../modules](../../modules)
against the LocalStack endpoint (`http://localhost:4566`), so the AWS
resources this platform depends on (S3 buckets, SQS queues, SNS topics,
Secrets Manager entries) are created the same way in local development as
they would be in a real account. Populated alongside the services that need
them.

## Usage

With `docker compose` running (LocalStack up on `localhost:4566` - see
[infrastructure/docker](../../docker)):

```bash
cd infrastructure/terraform/environments/local
terraform init
terraform apply
```

Currently provisions: a `booking-receipts` S3 bucket (`booking-service`
writes a JSON receipt to it on booking creation - see
[docs/adr/0008-use-localstack-and-terraform-for-aws-resources.md](../../../../docs/adr/0008-use-localstack-and-terraform-for-aws-resources.md)).
