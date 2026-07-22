variable "bucket_name" {
  description = "Globally-unique S3 bucket name."
  type        = string
}

variable "versioning_enabled" {
  description = "Whether object versioning is enabled - useful for buckets holding records that must never be silently overwritten (e.g. receipts)."
  type        = bool
  default     = false
}
