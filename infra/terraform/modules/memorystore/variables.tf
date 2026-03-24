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

variable "redis_tier" {
  type    = string
  default = "STANDARD_HA"
}

variable "network_id" {
  type = string
}
