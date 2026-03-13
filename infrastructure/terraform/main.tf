# ============================================
# LEGAL MANAGEMENT SYSTEM - AWS INFRASTRUCTURE
# Terraform Configuration
# ============================================

terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Backend para guardar el estado en S3
  backend "s3" {
    bucket         = "legal-system-terraform-s3"
    key            = "terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    use_lockfile = true
  }
}

provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "Legal Management System"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# ============================================
# VARIABLES
# ============================================

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment (dev, staging, prod)"
  type        = string
  default     = "prod"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "legal-case-system"
}

variable "db_username" {
  description = "Database master username"
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "Database master password"
  type        = string
  sensitive   = true
}
variable "domain_name" {
  description = "Dominio del sistema legal"
  type        = string
  default     = "ralrogolegal.com"
}

# ============================================
# VPC AND NETWORKING
# ============================================

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.1.2"

  name = "${var.project_name}-vpc"
  cidr = "10.0.0.0/16"


  azs             = ["${var.aws_region}a", "${var.aws_region}b", "${var.aws_region}c"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]
  database_subnets = ["10.0.201.0/24", "10.0.202.0/24"]

  enable_nat_gateway   = true
  single_nat_gateway   = var.environment == "prod" ? true : false
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Environment = var.environment
  }
}

# ============================================
# SECURITY GROUPS
# ============================================

# Security Group para RDS
resource "aws_security_group" "rds" {
  name        = "${var.project_name}-rds-sg"
  description = "Security group for RDS MySQL database"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "MySQL from EKS"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.eks_nodes.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-rds-sg"
  }
}

# Security Group para EKS Nodes
resource "aws_security_group" "eks_nodes" {
  name        = "${var.project_name}-eks-nodes-sg"
  description = "Security group for EKS worker nodes"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description = "Allow nodes to communicate with each other"
    from_port   = 0
    to_port     = 65535
    protocol    = "tcp"
    self        = true
  }

  ingress {
    description = "Allow pods to communicate with cluster API"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = [module.vpc.vpc_cidr_block]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-eks-nodes-sg"
  }
}

# ============================================
# RDS MYSQL DATABASE
# ============================================

resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = module.vpc.database_subnets

  tags = {
    Name = "${var.project_name}-db-subnet-group"
  }
}

resource "aws_db_instance" "mysql" {
  identifier     = "${var.project_name}-db"
  engine         = "mysql"
  engine_version = "8.0"
  
  instance_class    = var.environment == "prod" ? "db.t3.medium" : "db.t3.micro"
  allocated_storage = var.environment == "prod" ? 100 : 20
  storage_type      = "gp3"
  storage_encrypted = true

  db_name  = "legal_management_db"
  username = var.db_username
password = var.db_password
  port     = 3306

  multi_az               = var.environment == "prod" ? true : false
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = true

  backup_retention_period = var.environment == "prod" ? 7 : 1
  backup_window          = "03:00-04:00"
  maintenance_window     = "mon:04:00-mon:05:00"

  enabled_cloudwatch_logs_exports = ["error", "general", "slowquery"]
  
  skip_final_snapshot       = var.environment == "dev" ? true : false
  final_snapshot_identifier = var.environment == "dev" ? null : "${var.project_name}-final-snapshot-${formatdate("YYYY-MM-DD-hhmm", timestamp())}"

  tags = {
    Name = "${var.project_name}-mysql-db"
  }
}

# ============================================
# S3 BUCKETS
# ============================================

# Bucket para almacenamiento privado del Admin
resource "aws_s3_bucket" "admin_private" {
  bucket = "${var.project_name}-admin-private-${var.environment}-s3"

  tags = {
    Name        = "Admin Private Storage"
    Environment = var.environment
  }
}

resource "aws_s3_bucket_versioning" "admin_private" {
  bucket = aws_s3_bucket.admin_private.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "admin_private" {
  bucket = aws_s3_bucket.admin_private.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "admin_private" {
  bucket = aws_s3_bucket.admin_private.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Bucket para backups de base de datos
resource "aws_s3_bucket" "db_backups" {
  bucket = "${var.project_name}-db-backups-${var.environment}-s3"

  tags = {
    Name        = "Database Backups"
    Environment = var.environment
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "db_backups" {
  bucket = aws_s3_bucket.db_backups.id

  rule {
    id     = "delete-old-backups"
    status = "Enabled"
  filter {}  
    expiration {
      days = 30
    }
  }
}

# ============================================
# EKS CLUSTER
# ============================================

module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 19.0"

  cluster_name    = "${var.project_name}-cluster"
  cluster_version = "1.31"

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  cluster_endpoint_public_access = true
  manage_aws_auth_configmap       = false

  eks_managed_node_groups = {
    main = {
      name = "legal-nodes"

      instance_types = var.environment == "prod" ? ["t3.medium"] : ["t3.small"]
      capacity_type  = var.environment == "prod" ? "ON_DEMAND" : "SPOT"

      min_size     = var.environment == "prod" ? 2 : 1
      max_size     = var.environment == "prod" ? 4 : 2
      desired_size = var.environment == "prod" ? 3 : 1

      vpc_security_group_ids = [aws_security_group.eks_nodes.id]
    }
  }

  # Habilitar IRSA (IAM Roles for Service Accounts)
  enable_irsa = true

  tags = {
    Environment = var.environment
  }
}

# ============================================
# IAM ROLES Y POLÍTICAS
# ============================================

# IAM Role para EKS Pods acceder a S3
resource "aws_iam_role" "eks_s3_access" {
  name = "${var.project_name}-eks-s3-access"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRoleWithWebIdentity"
        Effect = "Allow"
        Principal = {
          Federated = module.eks.oidc_provider_arn
        }
        Condition = {
          StringEquals = {
            "${module.eks.oidc_provider}:sub" = "system:serviceaccount:legal-system-prod:document-service"
          }
        }
      }
    ]
  })
}

resource "aws_iam_role_policy" "eks_s3_policy" {
  name = "${var.project_name}-s3-policy"
  role = aws_iam_role.eks_s3_access.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.admin_private.arn,
          "${aws_s3_bucket.admin_private.arn}/*"
        ]
      }
    ]
  })
}

# IAM Role para Lambda
resource "aws_iam_role" "lambda_execution" {
  name = "${var.project_name}-lambda-execution"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# ============================================
# SES (Simple Email Service)
# ============================================

resource "aws_ses_email_identity" "noreply" {
  email = "noreply@${var.domain_name}"
}

# ============================================
# SNS (Simple Notification Service)
# ============================================

resource "aws_sns_topic" "notifications" {
  name = "${var.project_name}-notifications"

  tags = {
    Name = "Legal System Notifications"
  }
}

# ============================================
# CLOUDWATCH LOG GROUPS
# ============================================

resource "aws_cloudwatch_log_group" "application" {
  name              = "/aws/legal-system/${var.environment}"
  retention_in_days = var.environment == "prod" ? 30 : 7

  tags = {
    Environment = var.environment
  }
}

# ============================================
# ELASTICACHE REDIS (para caché)
# ============================================

resource "aws_elasticache_subnet_group" "redis" {
  name       = "${var.project_name}-redis-subnet"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_security_group" "redis" {
  name        = "${var.project_name}-redis-sg"
  description = "Security group for Redis cache"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "Redis from EKS"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.eks_nodes.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_elasticache_cluster" "redis" {
  cluster_id           = "${var.project_name}-redis"
  engine               = "redis"
  node_type            = var.environment == "prod" ? "cache.t3.medium" : "cache.t3.micro"
  num_cache_nodes      = 1
  parameter_group_name = "default.redis7"
  engine_version       = "7.0"
  port                 = 6379
  
  subnet_group_name  = aws_elasticache_subnet_group.redis.name
  security_group_ids = [aws_security_group.redis.id]

  tags = {
    Name = "${var.project_name}-redis"
  }
}

# ============================================
# OUTPUTS
# ============================================

output "vpc_id" {
  description = "ID of the VPC"
  value       = module.vpc.vpc_id
}

output "rds_endpoint" {
  description = "RDS instance endpoint"
  value       = aws_db_instance.mysql.endpoint
  sensitive   = true
}

output "eks_cluster_endpoint" {
  description = "Endpoint for EKS cluster"
  value       = module.eks.cluster_endpoint
}

output "eks_cluster_name" {
  description = "Name of the EKS cluster"
  value       = module.eks.cluster_name
}

output "s3_admin_bucket" {
  description = "S3 bucket for admin private storage"
  value       = aws_s3_bucket.admin_private.bucket
}

output "redis_endpoint" {
  description = "Redis cache endpoint"
  value       = aws_elasticache_cluster.redis.cache_nodes[0].address
}

output "sns_topic_arn" {
  description = "SNS topic ARN for notifications"
  value       = aws_sns_topic.notifications.arn
}
