# local environment

Terraform root module wiring the modules in [../../modules](../../modules)
against the LocalStack endpoint (`http://localhost:4566`), so the AWS
resources this platform depends on (S3 buckets, SQS queues, SNS topics,
Secrets Manager entries) are created the same way in local development as
they would be in a real account. Populated alongside the services that need
them.
