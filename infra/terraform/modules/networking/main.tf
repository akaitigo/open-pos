# VPC and subnet configuration for multi-region deployment

resource "google_compute_network" "vpc" {
  name                    = "openpos-${var.environment}-vpc"
  project                 = var.project_id
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "primary" {
  name          = "openpos-${var.environment}-primary"
  project       = var.project_id
  region        = var.primary_region
  network       = google_compute_network.vpc.id
  ip_cidr_range = "10.0.0.0/20"

  secondary_ip_range {
    range_name    = "pods"
    ip_cidr_range = "10.4.0.0/14"
  }

  secondary_ip_range {
    range_name    = "services"
    ip_cidr_range = "10.8.0.0/20"
  }

  private_ip_google_access = true
}

resource "google_compute_subnetwork" "secondary" {
  name          = "openpos-${var.environment}-secondary"
  project       = var.project_id
  region        = var.secondary_region
  network       = google_compute_network.vpc.id
  ip_cidr_range = "10.16.0.0/20"

  secondary_ip_range {
    range_name    = "pods"
    ip_cidr_range = "10.20.0.0/14"
  }

  secondary_ip_range {
    range_name    = "services"
    ip_cidr_range = "10.24.0.0/20"
  }

  private_ip_google_access = true
}

# Private services access for Cloud SQL
resource "google_compute_global_address" "private_ip_range" {
  name          = "openpos-${var.environment}-private-ip"
  project       = var.project_id
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 16
  network       = google_compute_network.vpc.id
}

resource "google_service_networking_connection" "private_vpc" {
  network                 = google_compute_network.vpc.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_ip_range.name]
}

# Cloud Router + NAT for outbound internet (GKE nodes)
resource "google_compute_router" "primary" {
  name    = "openpos-${var.environment}-router-primary"
  project = var.project_id
  region  = var.primary_region
  network = google_compute_network.vpc.id
}

resource "google_compute_router_nat" "primary" {
  name                               = "openpos-${var.environment}-nat-primary"
  project                            = var.project_id
  region                             = var.primary_region
  router                             = google_compute_router.primary.name
  nat_ip_allocate_option             = "AUTO_ONLY"
  source_subnetwork_ip_ranges_to_nat = "ALL_SUBNETWORKS_ALL_IP_RANGES"
}
