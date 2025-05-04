output "ssh_config_file" {
  value = abspath(local_file.ssh_config.filename)
}