resource "random_string" "test_id" {
  length  = 8
  upper   = false
  special = false
  numeric = false
}

resource "hcloud_volume" "hcloud_volume" {
  name              = "hcloud-volume-${random_string.test_id.id}"
  size              = 32
  location          = "nbg1"
  delete_protection = true
}

resource "hcloud_volume" "hcloud_volumes" {
  count             = 12
  name              = "hcloud-volume-${random_string.test_id.id}-${count.index}"
  size              = 32
  location          = "nbg1"
  delete_protection = true
}

resource "tls_private_key" "ssh_key1" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "local_file" "ssh_key1" {
  filename = "${path.module}/id_rsa"
  content  = tls_private_key.ssh_key1.private_key_openssh
}

resource "hcloud_uploaded_certificate" "hcloud_uploaded_certificate1" {
  name        = "hcloud-uploaded-certificate-${random_string.test_id.id}"
  certificate = <<-EOT
  -----BEGIN CERTIFICATE-----
  MIIGOzCCBSOgAwIBAgIMS+4BB2bl2pcyJiHAMA0GCSqGSIb3DQEBCwUAMEwxCzAJ
  BgNVBAYTAkJFMRkwFwYDVQQKExBHbG9iYWxTaWduIG52LXNhMSIwIAYDVQQDExlB
  bHBoYVNTTCBDQSAtIFNIQTI1NiAtIEcyMB4XDTIyMDUzMDE2MDAxMVoXDTIzMDcw
  MTE2MDAxMFowGTEXMBUGA1UEAwwOKi56c21hcnRleC5jb20wggEiMA0GCSqGSIb3
  DQEBAQUAA4IBDwAwggEKAoIBAQD5QsuEGzXbuHnMA2ycUmXm32l5ohU7/9YhL79D
  QmhQgNrjTVR+3K7KfFCq5BqcUoY12jClBZTOwjofHjtjK1qDIXBDIpynuVToMA4K
  NAqFVgzOXoalLy8i8bnU9oNAWWkLBmJ98o8Fg4DVFvxiCfUciXookQCnvvj2TqoO
  UPWc6PtJ9ZAIS0yeAVcqQ6p5Ah6jMXmvHnsDF8uPS8YYaEQ5k+UEYgkdj/xY1eHE
  jCCOWkOyIB13aarwI3qwNlQc/cM9sqKEBqKuk9G0A4lbQR00k8xsDAxpD4HKBPZ5
  2/HKp5+cjTY5WnPAvGw5w+HAELFbg0tT+cU3Jsm9jz+jO6s3AgMBAAGjggNOMIID
  SjAOBgNVHQ8BAf8EBAMCBaAwgYoGCCsGAQUFBwEBBH4wfDBDBggrBgEFBQcwAoY3
  aHR0cDovL3NlY3VyZS5nbG9iYWxzaWduLmNvbS9jYWNlcnQvZ3NhbHBoYXNoYTJn
  MnIxLmNydDA1BggrBgEFBQcwAYYpaHR0cDovL29jc3AyLmdsb2JhbHNpZ24uY29t
  L2dzYWxwaGFzaGEyZzIwVwYDVR0gBFAwTjBCBgorBgEEAaAyAQoKMDQwMgYIKwYB
  BQUHAgEWJmh0dHBzOi8vd3d3Lmdsb2JhbHNpZ24uY29tL3JlcG9zaXRvcnkvMAgG
  BmeBDAECATAJBgNVHRMEAjAAMD8GA1UdHwQ4MDYwNKAyoDCGLmh0dHA6Ly9jcmwu
  Z2xvYmFsc2lnbi5jb20vZ3MvZ3NhbHBoYXNoYTJnMi5jcmwwJwYDVR0RBCAwHoIO
  Ki56c21hcnRleC5jb22CDHpzbWFydGV4LmNvbTAdBgNVHSUEFjAUBggrBgEFBQcD
  AQYIKwYBBQUHAwIwHwYDVR0jBBgwFoAU9c3VPAhQ+WpPOreX2laD5mnSaPcwHQYD
  VR0OBBYEFPjYcQ+usjCDA+oTo6T0TSFzJjyyMIIBfAYKKwYBBAHWeQIEAgSCAWwE
  ggFoAWYAdgDoPtDaPvUGNTLnVyi8iWvJA9PL0RFr7Otp4Xd9bQa9bgAAAYEVskA4
  AAAEAwBHMEUCIDPjhQwz3xHjjY85TOw9DCViI7H6tPLDOmFGAZAx/yO3AiEA/kol
  LUuxmwd7ovZVZBSv4uIhmOJCyjjEkG4SUUMJ+wMAdQBvU3asMfAxGdiZAKRRFf93
  FRwR2QLBACkGjbIImjfZEwAAAYEVskAsAAAEAwBGMEQCICuB2UObxBUHsqR9o4UH
  nx+ZT5isi4YINUjEdVQYSOugAiBx53XqS/oMrvCLxSMfZwV03yYvOoYAB6AS+0KT
  zq/QpQB1AFWB1MIWkDYBSuoLm1c8U/DA5Dh4cCUIFy+jqh0HE9MMAAABgRWyQE0A
  AAQDAEYwRAIgMtmKBMYGxKKFWaz22YfQJ0bSW8YGFlwNcW1GNYGVhEQCICc362LG
  P54MdhC4HrevsptJIPHqYPOPwF0mEIW1tuAWMA0GCSqGSIb3DQEBCwUAA4IBAQAQ
  +DGy2XM89RgvyUHqMyXt+A9T1dxGEGpPfh7pXradqiiY6IE5l00mrIgzckEOJJeN
  OJFrR3KCnQG5/Ct8ydJP2/Ez0mVWtvSK8h+FXnfr9FaJClvu71uqAU1ytWZ8FFhK
  xyjF6+5K5VCWklZdUbego30mMspzETT5dUcSeXnKY8bRBGF1dCmQDDQSWacEBDac
  DyM24dFQ1LEpqP8fFeZhC55gmMSxrNlnf8mdLGX6x1vIpUIPDsiaJHJbblM5J3IN
  3mqBdUanH9B+lfcVWivfentnl6/7CBIc+7+3BinJzx2EDLnnadfz4DwaJX/ypIKs
  SFgHx2Bsj877cwYA/RD7
  -----END CERTIFICATE-----
  EOT
  private_key = <<-EOT
  -----BEGIN PRIVATE KEY-----
  MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQD5QsuEGzXbuHnM
  A2ycUmXm32l5ohU7/9YhL79DQmhQgNrjTVR+3K7KfFCq5BqcUoY12jClBZTOwjof
  HjtjK1qDIXBDIpynuVToMA4KNAqFVgzOXoalLy8i8bnU9oNAWWkLBmJ98o8Fg4DV
  FvxiCfUciXookQCnvvj2TqoOUPWc6PtJ9ZAIS0yeAVcqQ6p5Ah6jMXmvHnsDF8uP
  S8YYaEQ5k+UEYgkdj/xY1eHEjCCOWkOyIB13aarwI3qwNlQc/cM9sqKEBqKuk9G0
  A4lbQR00k8xsDAxpD4HKBPZ52/HKp5+cjTY5WnPAvGw5w+HAELFbg0tT+cU3Jsm9
  jz+jO6s3AgMBAAECggEAbV+O18/syRXgc9HI1aseRbkgmhux/5raBcPixAuepDx2
  T6j9+5CwLe9wohvnRVK8y2KLV83kJvl48XCdlH1QgRuqG/tTDBG5nQyBDJ8bQrio
  c+FsaY0TvNyes0DcBN92xTyu+R72/O9gF6C3a/l8kWINtUEzLWgR7FpGKnQB4jBH
  a0mXv/GMY30CF+RPXfPyhZl9aNRiiImBvayHa2g0rP1Bs43MXP6D61UyKNXaXMy8
  BfGdMQ9AQ3GNZd8L+Lylzj2QIUy0io46BL7d7ZQm3VGIvdLFjHJ6WLHQhlpGCsF4
  c01WTLzZuqdzJ+vuysTfjpCTasjuOFQgzu7L4t2N6QKBgQD9arDGdWURfMPb6vnM
  0kjCG/oWKTgBBIhPK2XuQ/kouuUzqaRxfVl61pwwJ9cHqHExUMmC3bDGeUVTmEfj
  KdgUWPVixwLypmpiVzYvUfiHoZhTJfCYXFtj4GwEyAohuEVvLtpe+FKWnt4u7+9m
  AAbzZwzcP9WfNTWHbZkNVi7t+wKBgQD7zUJtkKT5nAY/Q+Vyact0kW+bH4t7740S
  KK2ZKLbbtYCr2H9LqlX1Q91UTJi5aFFPGthcYtPT2wchf8fA9KMjA7l2Vi2JxuJd
  IY8la6ANm2Hm1G5EWYwF9IcjRzxOWVaYwVPbrYCgajk0HagXPQA9kFRo8R3vSJiO
  dgfsHoOe9QKBgFW49m1bnsGom7RTqwZvB6+puRIwBULK3rUxL/zGP41Yk1nDg93k
  EhWsbQ8ZGvL7NrcA5fl/tmGc+ieJ9p9QM0jGwtMUENo2EvyLFcgyCUkQD6/owJc5
  fqytaLzBUjQP2mT+y12e0Ikk2nG0Nh4h2jgR3tbOPRvq6t2R5FkPkzZrAoGBAOTg
  NAtHOM2yJnOHEZ4nz8lLEPkdeTnUCpSA6RqYSW330tdg2IQ8dhmT8DBZ11BgI9gV
  fKitJAIjyp2GellHhKmlDwUjXA0p/EPO50CKTVdQ73JTkU8LXh1joRpN++Dzj6UV
  xVWepZYqN4jJlCpbRkavVCp3UFBZ2mFTo+vZ6KWpAoGBAIBqTZ0JDptSw0HKMCMy
  gPB1Pjr6XZ85DVUep6lB94fnWjTopE4oNLfQJ18tA5egX4ZGNBhJxXostQM+FAln
  X1fZ8SwDO9vG0MJUHro4iT26+1pPT4C+/d65x0wgjdlPlhKbiHTzqB4OBILBsDwz
  J7m1TfZ7oGUE5mYccINSEGgu
  -----END PRIVATE KEY-----
  EOT
}

resource "hcloud_ssh_key" "hcloud_ssh_key1" {
  name       = "hcloud-ssh-key-${random_string.test_id.id}"
  public_key = tls_private_key.ssh_key1.public_key_openssh
}

resource "hcloud_firewall" "hcloud_firewall1" {
  name = "hcloud-firewall-${random_string.test_id.id}"

  rule {
    direction = "in"
    protocol  = "tcp"
    port      = "22"
    source_ips = [
      "0.0.0.0/0",
      "::/0"
    ]
  }

  rule {
    direction = "in"
    protocol  = "tcp"
    port      = "80"
    source_ips = [
      "0.0.0.0/0",
      "::/0"
    ]
  }
}

resource "hcloud_firewall_attachment" "hcloud_firewall_attachment" {
  firewall_id = hcloud_firewall.hcloud_firewall1.id
  server_ids = [hcloud_server.hcloud_server1.id]
}

resource "hcloud_floating_ip" "hcloud_floating_ip1" {
  name          = "hcloud-floating-ip-${random_string.test_id.id}"
  type          = "ipv4"
  home_location = "nbg1"
}

resource "hcloud_floating_ip_assignment" "hcloud_floating_ip_assignment" {
  floating_ip_id = hcloud_floating_ip.hcloud_floating_ip1.id
  server_id      = hcloud_server.hcloud_server1.id
}

resource "hcloud_load_balancer" "hcloud_load_balancer_asg1" {
  name               = "hcloud-load-balancer-asg1"
  load_balancer_type = "lb11"
  location           = "nbg1"
}

resource "hcloud_load_balancer" "hcloud_load_balancer_asg2" {
  name               = "hcloud-load-balancer-asg2"
  load_balancer_type = "lb11"
  location           = "nbg1"
}

resource "hcloud_load_balancer" "hcloud_load_balancer_asg3" {
  name               = "hcloud-load-balancer-asg3"
  load_balancer_type = "lb11"
  location           = "nbg1"
}

resource "hcloud_load_balancer_service" "hcloud_load_balancer_service_asg1" {
  load_balancer_id = hcloud_load_balancer.hcloud_load_balancer_asg1.id
  protocol         = "tcp"
  listen_port      = 80
  destination_port = 80
}

resource "hcloud_load_balancer_service" "hcloud_load_balancer_service_asg2" {
  load_balancer_id = hcloud_load_balancer.hcloud_load_balancer_asg2.id
  protocol         = "tcp"
  listen_port      = 80
  destination_port = 80
}

resource "hcloud_load_balancer_service" "hcloud_load_balancer_service_asg3" {
  load_balancer_id = hcloud_load_balancer.hcloud_load_balancer_asg3.id
  protocol         = "tcp"
  listen_port      = 80
  destination_port = 80
}

resource "hcloud_load_balancer_target" "hcloud_load_balancer_target_asg1" {
  load_balancer_id = hcloud_load_balancer.hcloud_load_balancer_asg1.id
  type             = "server"
  server_id        = hcloud_server.hcloud_server1.id
}

resource "hcloud_load_balancer_target" "hcloud_load_balancer_target_asg2" {
  load_balancer_id = hcloud_load_balancer.hcloud_load_balancer_asg2.id
  type             = "label_selector"
  label_selector   = "foo=bar"
}

resource "hcloud_server" "hcloud_server1" {
  name               = "hcloud-server1"
  server_type        = "cx22"
  image              = "debian-12"
  location           = "nbg1"
  ssh_keys = [hcloud_ssh_key.hcloud_ssh_key1.id]
  placement_group_id = hcloud_placement_group.hcloud_placement_group1.id
  user_data = templatefile("${path.module}/cloud_init.template", {})
  labels = {
    "blcks.de/user-data-checksum" : "yolo"
  }
}

resource "hcloud_server" "hcloud_server2" {
  name        = "hcloud-server2"
  server_type = "cx22"
  image       = "debian-12"
  location    = "nbg1"
  ssh_keys = [hcloud_ssh_key.hcloud_ssh_key1.id]
  user_data = templatefile("${path.module}/cloud_init.template", {})
  labels = { foo : "bar" }
}

resource "hcloud_server" "hcloud_server3" {
  name        = "hcloud-server3"
  server_type = "cx22"
  image       = "debian-12"
  location    = "nbg1"
  ssh_keys = [hcloud_ssh_key.hcloud_ssh_key1.id]
  user_data = templatefile("${path.module}/cloud_init.template", {})
}

resource "hcloud_network" "hcloud_network1" {
  ip_range = "10.0.0.0/16"
  name     = "hcloud-network-${random_string.test_id.id}"
}

resource "hcloud_server_network" "hcloud_server_network1" {
  server_id  = hcloud_server.hcloud_server1.id
  network_id = hcloud_network.hcloud_network1.id
}

resource "hcloud_network_subnet" "hcloud_network_subnet1" {
  ip_range     = "10.0.1.0/24"
  network_id   = hcloud_network.hcloud_network1.id
  network_zone = "eu-central"
  type         = "cloud"
}

resource "hcloud_volume_attachment" "hcloud_volume_attachment1" {
  server_id = hcloud_server.hcloud_server1.id
  volume_id = hcloud_volume.hcloud_volume.id
}

resource "hcloud_placement_group" "hcloud_placement_group" {
  name = "hcloud-placement-group-${random_string.test_id.id}"
  type = "spread"
}

resource "hcloud_primary_ip" "hcloud_primary_ip" {
  name          = "hcloud-primary-ip-${random_string.test_id.id}"
  datacenter    = "fsn1-dc14"
  type          = "ipv4"
  assignee_type = "server"
  auto_delete   = true
}

resource "hcloud_placement_group" "hcloud_placement_group1" {
  name = "hcloud-placement-group1"
  type = "spread"
}

resource "hcloud_snapshot" "hcloud_snapshot1" {
  server_id = hcloud_server.hcloud_server1.id
}
