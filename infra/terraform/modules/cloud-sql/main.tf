# Cloud SQL PostgreSQL with cross-region read replica

resource "google_sql_database_instance" "primary" {
  name             = "openpos-${var.environment}-primary"
  project          = var.project_id
  region           = var.primary_region
  database_version = "POSTGRES_17"

  settings {
    tier              = var.db_tier
    availability_type = "REGIONAL" # HA within region

    backup_configuration {
      enabled                        = true
      point_in_time_recovery_enabled = true
      start_time                     = "02:00" # JST 11:00
      transaction_log_retention_days = 7

      backup_retention_settings {
        retained_backups = 30
      }
    }

    ip_configuration {
      ipv4_enabled                                  = false
      private_network                               = var.network_id
      enable_private_path_for_google_cloud_services = true
    }

    database_flags {
      name  = "max_connections"
      value = "200"
    }

    database_flags {
      name  = "log_min_duration_statement"
      value = "1000" # Log queries > 1s
    }

    maintenance_window {
      day          = 7 # Sunday
      hour         = 3 # JST 12:00
      update_track = "stable"
    }

    insights_config {
      query_insights_enabled  = true
      query_plans_per_minute  = 5
      query_string_length     = 1024
      record_application_tags = true
      record_client_address   = true
    }

    user_labels = {
      environment = var.environment
      managed-by  = "terraform"
    }
  }

  deletion_protection = true
}

# Cross-region read replica (Osaka)
resource "google_sql_database_instance" "replica" {
  name                 = "openpos-${var.environment}-replica"
  project              = var.project_id
  region               = var.secondary_region
  database_version     = "POSTGRES_17"
  master_instance_name = google_sql_database_instance.primary.name

  replica_configuration {
    failover_target = true
  }

  settings {
    tier              = var.db_tier
    availability_type = "ZONAL"

    ip_configuration {
      ipv4_enabled                                  = false
      private_network                               = var.network_id
      enable_private_path_for_google_cloud_services = true
    }

    user_labels = {
      environment = var.environment
      role        = "replica"
      managed-by  = "terraform"
    }
  }

  deletion_protection = true
}

# Database
resource "google_sql_database" "openpos" {
  name     = "openpos"
  project  = var.project_id
  instance = google_sql_database_instance.primary.name
}

# Database user
resource "google_sql_user" "openpos" {
  name     = "openpos"
  project  = var.project_id
  instance = google_sql_database_instance.primary.name
  password = var.db_password
}
