terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# Targets LocalStack, not a real AWS account - see docs/adr/0008-use-localstack-and-terraform-for-aws-resources.md.
# The access/secret keys are the well-known LocalStack fake pair (test/test),
# not a credential leak - see SECURITY.md.
provider "aws" {
  region                      = "us-east-1"
  access_key                  = "test"
  secret_key                  = "test"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    s3 = "http://localhost:4566"
  }
}
