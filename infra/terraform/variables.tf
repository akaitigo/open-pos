variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "environment" {
  description = "Environment name (staging, production)"
  type        = string
  default     = "production"
}

variable "primary_region" {
  description = "Primary GCP region"
  type        = string
  default     = "asia-northeast1" # Tokyo
}

variable "secondary_region" {
  description = "Secondary GCP region for DR"
  type        = string
  default     = "asia-northeast2" # Osaka
}

variable "primary_node_count" {
  description = "Number of GKE nodes in primary cluster"
  type        = number
  default     = 3
}

variable "secondary_node_count" {
  description = "Number of GKE nodes in secondary cluster (standby)"
  type        = number
  default     = 1
}

variable "db_tier" {
  description = "Cloud SQL machine type"
  type        = string
  default     = "db-custom-2-8192"
}

variable "redis_tier" {
  description = "Memorystore Redis tier"
  type        = string
  default     = "STANDARD_HA"
}

variable "db_password" {
  description = "Cloud SQL database password"
  type        = string
  sensitive   = true
}
