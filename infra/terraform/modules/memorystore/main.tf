# Memorystore Redis instances for primary and secondary regions

resource "google_redis_instance" "primary" {
  name           = "openpos-${var.environment}-redis-primary"
  project        = var.project_id
  region         = var.primary_region
  tier           = var.redis_tier
  memory_size_gb = 4

  redis_version = "REDIS_7_2"

  authorized_network = var.network_id
  connect_mode       = "PRIVATE_SERVICE_ACCESS"

  redis_configs = {
    maxmemory-policy = "allkeys-lru"
    notify-keyspace-events = ""
  }

  maintenance_policy {
    weekly_maintenance_window {
      day = "SUNDAY"
      start_time {
        hours   = 3
        minutes = 0
      }
    }
  }

  labels = {
    environment = var.environment
    managed-by  = "terraform"
  }
}

resource "google_redis_instance" "secondary" {
  name           = "openpos-${var.environment}-redis-secondary"
  project        = var.project_id
  region         = var.secondary_region
  tier           = var.redis_tier
  memory_size_gb = 2

  redis_version = "REDIS_7_2"

  authorized_network = var.network_id
  connect_mode       = "PRIVATE_SERVICE_ACCESS"

  redis_configs = {
    maxmemory-policy = "allkeys-lru"
  }

  labels = {
    environment = var.environment
    role        = "secondary"
    managed-by  = "terraform"
  }
}
