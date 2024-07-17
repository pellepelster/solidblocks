locals {
  variables = var.solidblocks_base_url == null ? {} : {
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

${templatefile("${path.module}/acme-ssl.template", {
    acme_ssl : var.acme_ssl
  })}

EOT
}