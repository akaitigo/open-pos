output "gke_primary_endpoint" {
  description = "Primary GKE cluster endpoint"
  value       = module.gke_primary.cluster_endpoint
  sensitive   = true
}

output "gke_secondary_endpoint" {
  description = "Secondary GKE cluster endpoint"
  value       = module.gke_secondary.cluster_endpoint
  sensitive   = true
}

output "cloud_sql_primary_ip" {
  description = "Cloud SQL primary instance private IP"
  value       = module.cloud_sql.primary_private_ip
  sensitive   = true
}

output "cloud_sql_replica_ip" {
  description = "Cloud SQL replica instance private IP"
  value       = module.cloud_sql.replica_private_ip
  sensitive   = true
}

output "redis_primary_host" {
  description = "Primary Redis host"
  value       = module.memorystore.primary_host
  sensitive   = true
}

output "redis_secondary_host" {
  description = "Secondary Redis host"
  value       = module.memorystore.secondary_host
  sensitive   = true
}
