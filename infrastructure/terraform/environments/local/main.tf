# Local (LocalStack) environment: one entry per AWS resource a service needs, each a thin call
# into the reusable module in ../../modules. Real environments get their own environments/<env>
# directory reusing the same modules against the real provider - see
# docs/adr/0008-use-localstack-and-terraform-for-aws-resources.md.

module "booking_receipts_bucket" {
  source = "../../modules/s3"

  bucket_name        = "booking-receipts"
  versioning_enabled = true
}
