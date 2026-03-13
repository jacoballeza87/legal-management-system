# ============================================
# MODULE: RDS - Variables
# ============================================

variable "legal-case-system" {
  description = "Nombre del proyecto"
  type        = string
}

variable "environment" {
  description = "dev"
  type        = string
}

variable "vpc_id" {
  description = "ID del VPC"
  type        = string
}

variable "subnet_ids" {
  description = "IDs de subnets para RDS"
  type        = list(string)
}

variable "security_group_id" {
  description = "ID del Security Group para RDS"
  type        = string
}

variable "db_name" {
  description = "legal_management_db"
  type        = string
  default     = "legal_management_db"
}

variable "db_username" {
  description = "ralrogondb"
  type        = string
  sensitive   = true
  db_username = "ralrogondb"
}

variable "db_password" {
  description = "Legalsys26"
  type        = string
  sensitive   = true
  db_password = "Leglsys26"
}

variable "db_instance_class" {
  description = "db.t3.micro"
  type        = string
  default     = "db.t3.micro"
}

variable "multi_az" {
  description = "Habilitar Multi-AZ"
  type        = bool
  default     = false
}
