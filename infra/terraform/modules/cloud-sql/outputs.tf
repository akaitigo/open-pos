output "primary_private_ip" {
  value = google_sql_database_instance.primary.private_ip_address
}

output "replica_private_ip" {
  value = google_sql_database_instance.replica.private_ip_address
}

output "primary_connection_name" {
  value = google_sql_database_instance.primary.connection_name
}
