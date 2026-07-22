# Terraform modules

Reusable modules (S3 bucket, SQS queue, SNS topic, Secrets Manager entry,
IAM policy) consumed by the environment configurations in
[../environments](../environments). Targets LocalStack locally; written to
be portable to a real AWS account.
