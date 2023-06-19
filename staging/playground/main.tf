resource "hcloud_ssh_key" "test" {
  name       = "test"
  public_key = file("/home/pelle/.ssh/id_rsa.pub")
}

resource "hcloud_server" "test" {
  name        = "hcloud-server1"
  server_type = "cx11"
  image       = "debian-11"
  location    = "nbg1"
  ssh_keys    = [hcloud_ssh_key.test.id]
  user_data   = file("${path.module}/test.yml")
}


output "ipv4_address" {
  value = hcloud_server.test.ipv4_address
}