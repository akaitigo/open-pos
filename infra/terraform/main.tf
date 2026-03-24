# open-pos Multi-Region DR Infrastructure
# Primary: asia-northeast1 (Tokyo)
# Secondary: asia-northeast2 (Osaka)

terraform {
  required_version = ">= 1.6.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
    google-beta = {
      source  = "hashicorp/google-beta"
      version = "~> 5.0"
    }
  }

  backend "gcs" {
    bucket = "openpos-terraform-state"
    prefix = "infra"
  }
}

provider "google" {
  project = var.project_id
  region  = var.primary_region
}

provider "google-beta" {
  project = var.project_id
  region  = var.primary_region
}

# --- Networking ---
module "networking" {
  source = "./modules/networking"

  project_id       = var.project_id
  primary_region   = var.primary_region
  secondary_region = var.secondary_region
  environment      = var.environment
}

# --- GKE Clusters ---
module "gke_primary" {
  source = "./modules/gke"

  project_id   = var.project_id
  region       = var.primary_region
  environment  = var.environment
  cluster_name = "openpos-${var.environment}-primary"
  network_id   = module.networking.vpc_id
  subnet_id    = module.networking.primary_subnet_id
  node_count   = var.primary_node_count

  depends_on = [module.networking]
}

module "gke_secondary" {
  source = "./modules/gke"

  project_id   = var.project_id
  region       = var.secondary_region
  environment  = var.environment
  cluster_name = "openpos-${var.environment}-secondary"
  network_id   = module.networking.vpc_id
  subnet_id    = module.networking.secondary_subnet_id
  node_count   = var.secondary_node_count

  depends_on = [module.networking]
}

# --- Cloud SQL (Primary + Cross-Region Read Replica) ---
module "cloud_sql" {
  source = "./modules/cloud-sql"

  project_id       = var.project_id
  primary_region   = var.primary_region
  secondary_region = var.secondary_region
  environment      = var.environment
  db_tier          = var.db_tier
  network_id       = module.networking.vpc_id

  depends_on = [module.networking]
}

# --- Memorystore (Redis) ---
module "memorystore" {
  source = "./modules/memorystore"

  project_id       = var.project_id
  primary_region   = var.primary_region
  secondary_region = var.secondary_region
  environment      = var.environment
  redis_tier       = var.redis_tier
  network_id       = module.networking.vpc_id

  depends_on = [module.networking]
}
