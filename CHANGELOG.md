# Changelog

## v0.1.12

**IMPROVEMENTS**

* update Solidblocks versions for Hetzner RDS
* relax version requirements for Hetzner RDS
* fix Hetzner RDS module in Terraform registry

## v0.1.11

**IMPROVEMENTS**

* add Terraform state backend initialization helpers, see https://pellepelster.github.io/solidblocks/terraform

## v0.1.10

* test for release process

## v0.1.8

**IMPROVEMENTS**

* add private ip address as output to Hetzner RDS postgresql module
* make sure new repositories are created after backup config changes

## v0.1.6

**IMPROVEMENTS**

* add options to inject pre- / post-configuration scripts into Hetzner RDS postgresql module
* add fine-grained network settings controls for Hetzner RDS postgresql module

**DEPRECATIONS**

* `extra_user_data`will be deprecated in favor of `pre_script`/`post_script` for Hetzner RDS postgresql module