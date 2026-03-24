variable "project_id" {
  type = string
}

variable "primary_region" {
  type = string
}

variable "secondary_region" {
  type = string
}

variable "environment" {
  type = string
}

variable "db_tier" {
  type    = string
  default = "db-custom-2-8192"
}

variable "network_id" {
  type = string
}

variable "db_password" {
  type      = string
  sensitive = true
  default   = "" # Set via GCP Secret Manager
}
