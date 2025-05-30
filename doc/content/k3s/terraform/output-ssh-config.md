+++
title = 'Output SSH config'
description = "Create SSH config and identity file for SSH access to VMs"
+++

<!-- BEGIN_TF_DOCS -->
## Requirements

No requirements.

## Providers

| Name | Version |
|------|---------|
| <a name="provider_local"></a> [local](#provider\_local) | n/a |

## Resources

| Name | Type |
|------|------|
| [local_file.ssh_client_identity](https://registry.terraform.io/providers/hashicorp/local/latest/docs/resources/file) | resource |
| [local_file.ssh_config](https://registry.terraform.io/providers/hashicorp/local/latest/docs/resources/file) | resource |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| <a name="input_output_path"></a> [output\_path](#input\_output\_path) | path for the generated files | `string` | n/a | yes |
| <a name="input_ssh_private_key_openssh"></a> [ssh\_private\_key\_openssh](#input\_ssh\_private\_key\_openssh) | public key for server access in OpenSSH format | `string` | n/a | yes |
| <a name="input_ssh_servers"></a> [ssh\_servers](#input\_ssh\_servers) | list of servers that can be accessed via the supplied `ssh_private_key_openssh` | <pre>list(object({<br/>    name         = string,<br/>    user         = optional(string, "root"),<br/>    ipv4_address = string<br/>  }))</pre> | n/a | yes |

## Outputs

| Name | Description |
|------|-------------|
| <a name="output_ssh_config_file"></a> [ssh\_config\_file](#output\_ssh\_config\_file) | full path for the generated openssh client configuration |
<!-- END_TF_DOCS -->