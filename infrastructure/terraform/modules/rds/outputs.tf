# ============================================
# MODULE: RDS - Outputs
# ============================================

output "rds_endpoint" {
  description = "Endpoint del RDS"
  value       = aws_db_instance.mysql.endpoint
  sensitive   = true
}

output "rds_address" {
  description = "Host del RDS"
  value       = aws_db_instance.mysql.address
  sensitive   = true
}

output "rds_port" {
  description = "Puerto del RDS"
  value       = aws_db_instance.mysql.port
}

output "db_name" {
  description = "Nombre de la base de datos"
  value       = aws_db_instance.mysql.db_name
}

output "rds_identifier" {
  description = "Identificador de la instancia RDS"
  value       = aws_db_instance.mysql.identifier
}
