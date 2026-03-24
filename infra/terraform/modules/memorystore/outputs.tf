output "primary_host" {
  value = google_redis_instance.primary.host
}

output "primary_port" {
  value = google_redis_instance.primary.port
}

output "secondary_host" {
  value = google_redis_instance.secondary.host
}

output "secondary_port" {
  value = google_redis_instance.secondary.port
}
