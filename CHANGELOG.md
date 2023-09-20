# Changelog

## v0.1.21

**IMPROVEMENTS**

* add module parameter to add config options to generated [postgres.conf](https://pellepelster.github.io/solidblocks/hetzner/rds/#input%5c_postgres%5c_extra%5c_config)

**BUGFIXES**

* ensure all configured backup repositories are used when starting backups (fill/incremental/differential)



## v0.1.19

**BUGFIXES**

* fix broken v0.1.17 release

## v0.1.18

**IMPROVEMENTS**

* Support [encrypted backups](https://pellepelster.github.io/solidblocks/hetzner/rds-postgresql/#input\_backup\_encryption\_passphrase) for Hetzner RDS PostgreSQL module 
* Enable seamless version upgrades in RDS PostgreSQL modules (standalone docker and Hetzner module)
  * [RDS PostgreSQL backup encryption](https://pellepelster.github.io/solidblocks/rds/#global) 
  * [Hetzner RDS PostgreSQL backup encryption](https://pellepelster.github.io/solidblocks/hetzner/rds/#input\_postgres\_major\_version)
* Add maintenance mode in RDS PostgreSQL modules (standalone docker and Hetzner module)
  * [RDS PostgreSQL maintenance mode](https://pellepelster.github.io/solidblocks/rds/#maintenance)
  * [Hetzner RDS PostgreSQL maintenance mode](https://pellepelster.github.io/solidblocks/hetzner/rds/#maintenance)
* clean up and [document](https://pellepelster.github.io/solidblocks/rds/#architecture) RDS PostgreSQL module startup behavior 

## v0.1.17

**IMPROVEMENTS**

* [LetsEncrypt module](https://pellepelster.github.io/solidblocks/cloud-init/lego/) for cloud-init script library 
* LetsEncrypt support for Hetzner RDS Postgres module to support [automatic SSL provisioning](https://pellepelster.github.io/solidblocks/hetzner/rds/#input\_ssl\_enable) for PostgreSQL databases

## v0.1.16

**IMPROVEMENTS**

* add support for ARM instances to Hetzner RDS module
* clarify behaviour for Hetzner RDS database configuration
  * document available [extensions](https://pellepelster.github.io/solidblocks/rds/#extensions)
  * https://pellepelster.github.io/solidblocks/rds/#databases
  * https://pellepelster.github.io/solidblocks/rds/#input%5c_databases
* add automatic system updates to Hetzner RDS module using [unattended-upgrades](https://wiki.debian.org/UnattendedUpgrades)

**IMPROVEMENTS**

## v0.1.15

**BUGFIXES**

* fix nuke command for hetzner-nuker

**IMPROVEMENTS**

* update documentation
* verify private network snippets for Hetzner RDS module

## v0.1.14

**IMPROVEMENTS**

* update versions and fix documentation

## ~~v0.1.13~~

**REVOKED**

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