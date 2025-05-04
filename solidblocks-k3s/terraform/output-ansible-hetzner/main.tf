resource "local_file" "variables" {
  filename = "${var.output_path}/ansible/blcks_k3s_hetzner_variables.yml"
  content  = templatefile("${path.module}/templates/ansible_variables.template", {
    network_id : var.network_id
    hcloud_token : var.hcloud_token
  })
}
