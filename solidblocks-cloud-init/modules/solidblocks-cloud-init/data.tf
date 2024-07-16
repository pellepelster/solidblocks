data "local_file" "cloud_init_header" {
  filename = "${path.module}/cloud-init-header.sh"
}

data "local_file" "solidblocks_cloud_init_bootstrap" {
  filename = "${path.module}/solidblocks-cloud-init-bootstrap.sh"
}
