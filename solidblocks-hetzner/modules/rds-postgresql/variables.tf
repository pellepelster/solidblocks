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

variable "db_backup_gcs_bucket" {
  type        = string
  description = "Name of the Google Cloud storage bucket"
  default     = null
}

variable "db_backup_gcs_service_key" {
  type        = string
  description = "content of the service key json file with appropriate permissions to write to the `db_backup_gcs_bucket` bucket."
  default     = null
}

variable "backup_s3_bucket" {
  type        = string
  description = "AWS bucket name for S3 backups. To enable S3 backups `backup_s3_bucket`, `backup_s3_access_key` and `backup_s3_secret_key` have to be provided."
  default     = null
}

variable "restore_pitr" {
  type        = string
  description = "Point in time to recover to, using the recovery type `time` as defined in https://pgbackrest.org/command.html#command-restore. Format should be `YYYY-MM-dd HH:mm:ssz` Please be aware that the server hosting the database might be in a different timezone, so always include the timezone when specifying PITR times `date +\"%Y-%m-%d %H:%M:%S%z\"`"
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

variable "backup_s3_region" {
  type        = string
  description = "AWS region for S3 backups."
  default     = "eu-central-1"
}

variable "backup_s3_host" {
  type        = string
  description = "AWS host S3 backups."
  default     = "s3.eu-central-1.amazonaws.com"
}

variable "backup_s3_retention_full_type" {
  type        = string
  description = "AWS S3 backups retention policy type [count, time]. See https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-full"
  default     = "count"

  validation {
    condition     = var.backup_s3_retention_full_type != "count" || var.backup_s3_retention_full_type != "time"
    error_message = "only 'count' or 'time' is supported"
  }
}

variable "backup_s3_retention_full" {
  type        = number
  description = "AWS S3 backups full backup retention count/time. See https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-full"
  default     = 7
}

variable "backup_s3_retention_diff" {
  type        = number
  description = "AWS S3 backup number of differential backups to retain. See https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-diff"
  default     = 4
}

variable "backup_local_retention_full_type" {
  type        = string
  description = "Local backups retention policy type [count, time]. See https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-full"
  default     = "count"

  validation {
    condition     = var.backup_local_retention_full_type != "count" || var.backup_local_retention_full_type != "time"
    error_message = "only 'count' or 'time' is supported"
  }
}

variable "backup_local_retention_full" {
  type        = number
  description = "Local backups full backup retention count/time. See https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-full"
  default     = 7
}

variable "backup_local_retention_diff" {
  type        = number
  description = "Local backup number of differential backups to retain. See https://pgbackrest.org/configuration.html#section-repository/option-repo-retention-diff"
  default     = 4
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
  sensitive   = true
  description = "A list of databases to create when the instance is initialized, for example: `{ id : \"database1\", user : \"user1\", password : \"password1\" }`. Changing `user` and `password` is supported at any time, the provided config is translated into an config for the Solidblocks RDS PostgreSQL module (https://pellepelster.github.io/solidblocks/rds/index.html), please see https://pellepelster.github.io/solidblocks/rds/index.html#databases for more details of the database configuration."
}

variable "environment_variables" {
  type        = map(string)
  description = "A list environment variables to pass to the PostgreSQL  docker container"
  default     = {}
}


variable "db_admin_password" {
  type        = string
  sensitive   = true
  default     = ""
  description = "The database admin password. Username is always rds"
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

variable "postgres_stop_timeout" {
  type        = number
  description = "shutdown timeout for the postgres database in seconds, see also https://www.postgresql.org/docs/current/app-pg-ctl.html"
  default     = 60
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
  description = "ip address in the attached network. when an ip address is provided the database server will automatically be bound to this ip and will not be exposed on any other network interfaces"
  default     = null
}

variable "ssl_enable" {
  type        = bool
  description = "enable automatic ssl certificate creation using LetsEncrypt"
  default     = false
}

variable "firewall_disable" {
  type        = bool
  description = "disable automatic firewall configuration"
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

variable "ssl_acme_server" {
  type        = string
  description = "The URL of the ACME Server to use. Defaults to Let's Encrypt production environment."
  # LetsEncrypt Staging: https://acme-staging-v02.api.letsencrypt.org/directory
  default     = "https://acme-v02.api.letsencrypt.org/directory"
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
  description = "Solidblocks cloud-init version to use"
  default     = "0.2.6-pre5" #solidblocks_cloud_init_version
}

variable "solidblocks_rds_version" {
  type        = string
  description = "Solidblocks rds-postgresql version to use"
  default     = "0.2.6-pre5" #solidblocks_rds_version
}
