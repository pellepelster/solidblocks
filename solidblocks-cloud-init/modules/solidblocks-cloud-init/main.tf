locals {
  variables = {
    SOLIDBLOCKS_BASE_URL : var.solidblocks_base_url
  }

  user_data = <<EOT
${data.local_file.cloud_init_header.content}

${templatefile("${path.module}/variables.template", {
    variables : local.variables
  })}

${data.local_file.solidblocks_cloud_init_bootstrap.content}

${templatefile("${path.module}/storage-mounts.template", {
    storage : var.storage
  })}

EOT
}

#lego_setup_dns "${SSL_PATH}" "${SSL_EMAIL}" "${SSL_DOMAINS}" "${SSL_DNS_PROVIDER}" "/bin/true" "https://acme-staging-v02.api.letsencrypt.org/directory"