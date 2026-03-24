output "vpc_id" {
  value = google_compute_network.vpc.id
}

output "primary_subnet_id" {
  value = google_compute_subnetwork.primary.id
}

output "secondary_subnet_id" {
  value = google_compute_subnetwork.secondary.id
}
