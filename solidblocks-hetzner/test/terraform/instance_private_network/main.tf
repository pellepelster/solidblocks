resource "hcloud_volume" "data" {
  name     = "test-data-${var.test_id}"
  size     = 32
  format   = "ext4"
  location = var.location
}

resource "hcloud_network" "network1" {
  ip_range = "10.0.0.0/16"
  name     = "rds-postgresql-${var.test_id}"
}

resource "hcloud_network_subnet" "subnet1" {
  ip_range     = "10.0.1.0/24"
  network_id   = hcloud_network.network1.id
  network_zone = "eu-central"
  type         = "cloud"
}

resource "hcloud_server" "jumphost" {
  name        = "jumphost-${var.test_id}"
  image       = "debian-11"
  server_type = "cx22"

  ssh_keys = [data.hcloud_ssh_key.ssh_key.id]

  public_net {
    ipv4_enabled = true
  }

  network {
    network_id = hcloud_network.network1.id
    ip         = "10.0.1.5"
  }

  user_data = file("${path.module}/jumphost_user_data.sh")
}

module "rds-postgresql-1" {
  source = "../../../modules/rds-postgresql"
  name   = "rds-postgresql-${var.test_id}"

  location = var.location
  ssh_keys = [data.hcloud_ssh_key.ssh_key.id]

  data_volume   = hcloud_volume.data.id
  backup_volume = data.hcloud_volume.backup.id

  solidblocks_base_url           = "https://${data.aws_s3_bucket.bootstrap.bucket_domain_name}"
  solidblocks_cloud_init_version = var.solidblocks_version
  solidblocks_rds_version        = "${var.solidblocks_version}-rc"

  network_id = hcloud_network.network1.id
  network_ip = "10.0.1.4"

  databases = [
    { id : "database1", user : "user1", password : "password1" }
  ]

  depends_on = [hcloud_network.network1, hcloud_network_subnet.subnet1]
}