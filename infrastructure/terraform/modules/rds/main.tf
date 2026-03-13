e# ============================================
# MODULE: RDS MySQL
# Legal Management System
# ============================================

resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = var.subnet_ids

  tags = {
    Name        = "${var.project_name}-db-subnet-group"
    Environment = var.environment
  }
}

resource "aws_db_instance" "mysql" {
  identifier     = "${var.project_name}-db"
  engine         = "mysql"
  engine_version = "8.0.35"

  instance_class    = var.environment == "prod" ? "db.t3.medium" : "db.t3.micro"
  allocated_storage = var.environment == "prod" ? 100 : 20
  storage_type      = "gp3"
  storage_encrypted = true

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password
  port     = 3306

  multi_az               = var.environment == "prod" ? true : false
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [var.security_group_id]
  publicly_accessible    = true

  backup_retention_period = var.environment == "prod" ? 7 : 1
  backup_window           = "03:00-04:00"
  maintenance_window      = "mon:04:00-mon:05:00"

  enabled_cloudwatch_logs_exports = ["error", "general", "slowquery"]

  skip_final_snapshot       = var.environment == "dev" ? true : false
  final_snapshot_identifier = var.environment == "dev" ? null : "${var.project_name}-final-snapshot"

  tags = {
    Name        = "${var.project_name}-mysql-db"
    Environment = var.environment
  }
}
