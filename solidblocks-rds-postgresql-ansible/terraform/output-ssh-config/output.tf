output "ssh_config_file" {
  value       = abspath(local_file.ssh_config.filename)
  description = "full path for the generated openssh client configuration"
}