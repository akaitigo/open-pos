# GKE Autopilot cluster

resource "google_container_cluster" "cluster" {
  provider = google-beta

  name     = var.cluster_name
  project  = var.project_id
  location = var.region

  # Autopilot mode
  enable_autopilot = true

  network    = var.network_id
  subnetwork = var.subnet_id

  ip_allocation_policy {
    cluster_secondary_range_name  = "pods"
    services_secondary_range_name = "services"
  }

  private_cluster_config {
    enable_private_nodes    = true
    enable_private_endpoint = false
    master_ipv4_cidr_block  = "172.16.0.0/28"
  }

  release_channel {
    channel = "REGULAR"
  }

  maintenance_policy {
    recurring_window {
      start_time = "2026-01-01T03:00:00Z" # JST 12:00
      end_time   = "2026-01-01T07:00:00Z" # JST 16:00
      recurrence = "FREQ=WEEKLY;BYDAY=SU"
    }
  }

  resource_labels = {
    environment = var.environment
    managed-by  = "terraform"
  }
}
