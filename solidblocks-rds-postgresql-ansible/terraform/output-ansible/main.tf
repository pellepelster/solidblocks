resource "local_file" "inventory" {
  filename = "${var.output_path}/ansible/blcks_rds_postgres_inventory.yml"
  content = templatefile("${path.module}/templates/ansible_inventory.template", {
    databases       = var.databases
    ssh_config_file = var.ssh_config_file
  })
}

resource "local_file" "variables" {
  filename = "${var.output_path}/ansible/blcks_rds_postgres_variables.yml"
  content = templatefile("${path.module}/templates/ansible_variables.template", {
    backup_s3_endpoint   = var.backup_s3_endpoint
    backup_s3_bucket     = var.backup_s3_bucket
    backup_s3_key        = var.backup_s3_key
    backup_s3_key_secret = var.backup_s3_key_secret
    backup_s3_region     = var.backup_s3_region
    backup_s3_uri_style  = var.backup_s3_uri_style
  })
}

