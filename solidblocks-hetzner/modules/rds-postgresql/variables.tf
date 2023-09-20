variable "name" {
  type        = string
  description = "Unique name for the PostgreSQL instance"
}

variable "location" {
  type        = string
  description = "Hetzner location to use for provisioned resources"
}

variable "ssh_keys" {
  type        = list(number)
  description = "ssh keys to provision for instance access"
}

variable "server_type" {
  type        = string
  description = "Hetzner cloud server type, supports x86 and ARM architectures"
  default     = "cx11"
}

variable "data_volume" {
  type        = number
  description = "data volume id"
}

variable "backup_volume" {
  type        = string
  description = "backup volume id"
  default     = 0
}

variable "mode" {
  type        = string
  description = "startup mode for the database, can be empty to start the database or 'maintenance' to enable the maintenance mode of the underlying docker container to debug issues see also https://pellepelster.github.io/solidblocks//rds/#maintenance"
  default     = null

  validation {
    condition     = var.mode != null || var.mode != "maintenance"
    error_message = "currently only 'maintenance' or default is supported"
  }
}

variable "backup_s3_bucket" {
  type        = string
  description = "AWS bucket name for S3 backups. To enable S3 backups `backup_s3_bucket`, `backup_s3_access_key` and `backup_s3_secret_key` have to be provided."
  default     = null
}

variable "backup_s3_access_key" {
  type        = string
  description = "AWS access key for S3 backups. To enable S3 backups `backup_s3_bucket`, `backup_s3_access_key` and `backup_s3_secret_key` have to be provided."
  default     = null
}

variable "backup_s3_secret_key" {
  type        = string
  description = "AWS secret key for S3 backups. To enable S3 backups `backup_s3_bucket` `backup_s3_access_key` and `backup_s3_secret_key` have to be provided."
  default     = null
}

variable "backup_full_calendar" {
  type        = string
  description = "systemd timer spec for full backups"
  default     = "*-*-* 20:00:00"
}

variable "backup_incr_calendar" {
  type        = string
  description = "systemd timer spec for incremental backups"
  default     = "*-*-* *:00:55"
}

variable "databases" {
  type        = list(object({ id : string, user : string, password : string }))
  description = "A list of databases to create when the instance is initialized, for example: `{ id : \"database1\", user : \"user1\", password : \"password1\" }`. Changing `user` and `password` is supported at any time, the provided config is translated into an config for the Solidblocks RDS PostgreSQL module (https://pellepelster.github.io/solidblocks/rds/index.html), please see https://pellepelster.github.io/solidblocks/rds/index.html#databases for more details of the database configuration."
}

variable "postgres_major_version" {
  type        = number
  description = "PostgreSQL major version to use. Upgrading the version will trigger auto migration based on the underlying RDS PostgreSQL docker image, see also https://pellepelster.github.io/solidblocks/rds/index.html#versions. Please be aware that depending on the amount of data to migrate the migration may Terraforms timeouts, see https://pellepelster.github.io/solidblocks/hetzner/rds-postgresql/index.html#operations for debugging options."
  default     = 14

  validation {
    condition     = var.postgres_major_version != 14 || var.postgres_major_version != 15
    error_message = "currently only version 14 or 15 is supported"
  }
}

variable "postgres_extra_config" {
  type        = string
  description = "Extra postgres configurations options for the postgresql.conf, see also https://pellepelster.github.io/solidblocks/rds/index.html#global -> DB_POSTGRES_EXTRA_CONFIG"
  default     = null
}

variable "extra_user_data" {
  type        = string
  description = "deprecated, please use pre_script/post_script"
  default     = ""
}

variable "post_script" {
  type        = string
  description = "shell script that will be executed after the server configuration is executed"
  default     = ""
}

variable "pre_script" {
  type        = string
  description = "shell script that will be executed before the server configuration is executed"
  default     = ""
}

variable "labels" {
  type        = map(any)
  description = "A list of labels to be attached to the server instance."
  default     = {}
}

variable "public_net_ipv4_enabled" {
  type        = bool
  description = "enable/disable public ip addresses, see also https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/server#public_net"
  default     = true
}

variable "public_net_ipv6_enabled" {
  type        = bool
  description = "enable/disable public ip addresses, see also https://registry.terraform.io/providers/hetznercloud/hcloud/latest/docs/resources/server#public_net"
  default     = true
}

variable "network_id" {
  type        = number
  description = "network the created sever should be attached to, network_ip also needs to bet set in that case"
  default     = null
}

variable "network_ip" {
  type        = string
  description = "ip address in the attached network"
  default     = null
}

variable "ssl_enable" {
  type        = bool
  description = "enable automatic ssl certificate creation using LetsEncrypt"
  default     = false
}

variable "ssl_email" {
  type        = string
  description = "email address to use for LetsEncrypt account creation"
  default     = ""
}

variable "ssl_domains" {
  type        = list(string)
  description = "domains to use for generated certificates"
  default     = []

}

variable "ssl_dns_provider" {
  type        = string
  description = "provider type to use for LetsEncrypt DNS challenge, see https://go-acme.github.io/lego/dns/ for available options"
  default     = ""
}

variable "ssl_dns_provider_config" {
  type        = map(string)
  description = "environment configuration variable(s) to use for DNS provider selected via `ssl_dns_provider`, see documentation of selected provider for required configuration variables"
  default     = {}
}

variable "backup_encryption_passphrase" {
  type        = string
  description = "If set the backups will be encrypted using this passphrase"
  default     = null
}

variable "solidblocks_base_url" {
  type        = string
  default     = "https://github.com"
  description = "override base url for testing purposes"
}

variable "solidblocks_cloud_init_version" {
  type        = string
  description = "used for integration tests to inject test versions"
  default     = "v0.1.21"
}

variable "solidblocks_rds_version" {
  type        = string
  description = "used for integration tests to inject test versions"
  default     = "v0.1.21"
}

