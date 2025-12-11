resource "hcloud_primary_ip" "ipv4" {
  type          = "ipv4"
  name          = "${var.name}-ipv4"
  assignee_type = "server"
  auto_delete   = false
  datacenter    = var.datacenter
}

resource "hcloud_primary_ip" "ipv6" {
  type          = "ipv6"
  name          = "${var.name}-ipv6"
  assignee_type = "server"
  auto_delete   = false
  datacenter    = var.datacenter
}
