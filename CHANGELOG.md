# Changelog

## v0.4.3

* documentation updates

## v0.4.2

* release engineering

## v0.4.1

* release engineering

## v0.3.3

**IMPROVEMENTS**

* add option to provide database initialization options via `DB_CREATE_OPTIONS_*`, see [RDS PostgreSQL container](https://pellepelster.github.io/solidblocks/rds/#databases)

## v0.3.2

**IMPROVEMENTS**

* [RDS PostgreSQL container](https://pellepelster.github.io/solidblocks/rds/usage/#operations)
  * add PostgreSQL version 16/17
  * add timestamps to Solidblocks logging

## v0.3.1

**IMPROVEMENTS**

* Add command to indicate when provisioning is finished to [RDS PostgreSQL container](https://pellepelster.github.io/solidblocks/rds/usage/#operations)

## v0.3.0

**IMPROVEMENTS**

* Integrate snapshot and placement group deletion into CLI, see [PR #46](https://github.com/pellepelster/solidblocks/pull/46)

## v0.2.9

**IMPROVEMENTS**

* Add [Hetzner DNS library](https://pellepelster.github.io/solidblocks/hetzner/dns/)

## v0.2.8

**IMPROVEMENTS**

* Add [Hetzner nuke tool](https://pellepelster.github.io/solidblocks/cli/nuke/)

## v0.2.7

**BUGFIXES**

* Unescape newlines for database extra config in PostgreSQL RDS module   

**IMPROVEMENTS**

* Add infrastructure testing library to improve infrastructure integration tests, see als [this post](https://pelle.io/posts/solidblocks-test/) 


## v0.2.6

**IMPROVEMENTS**

* Add cloud init terraform module [documentation](https://pellepelster.github.io/solidblocks/cloud-init/).   

* **BUGFIXES**

* ensure schema permissions are set correctly for PostgreSQL version 15.x

## v0.2.5

**IMPROVEMENTS**

* add support for Google storage bucket based backups in Hetzner RDS module, see [documentation](https://pellepelster.github.io/solidblocks/hetzner/rds/#restore).   

## v0.2.4

**IMPROVEMENTS**

* providing a private network ip via [network_ip](https://pellepelster.github.io/solidblocks/hetzner/rds/#input\_network\_ip) in Hetzner RDS PostgreSQL will automatically bind the database to this ip only
* allow PITR recovery for Hetzner RDS module, see [documentation](https://pellepelster.github.io/solidblocks/hetzner/rds/#restore).

## v0.2.3

**BUGFIXES**

* fix retention configuration for local backups in Hetzner RDS PostgreSQL

## v0.2.2

* fix broken v0.2.1 release

## v0.2.1

**IMPROVEMENTS**

* allow setting db admin password via [db_admin_password](https://pellepelster.github.io/solidblocks/hetzner/rds/#input\_db\_admin\_password) in Hetzner RDS PostgreSQL
* allow overriding of backup retention times for full and diff backups via [backup_[s3|local]\_retention_*](https://pellepelster.github.io/solidblocks/hetzner/rds/#input\_backup\_s3\_retention\_diff) in Hetzner RDS PostgreSQL
* allow overriding of ACME server url via [ssl_acme_server](https://pellepelster.github.io/solidblocks/hetzner/rds/#input\_ssl\_acme\_server) in Hetzner RDS PostgreSQL
* make ipv6 address available as output for Hetzner RDS PostgreSQL

**BUGFIXES**

* backup jobs not properly started and only ran after reboot for Hetzner RDS PostgreSQL

## v0.2.0

**IMPROVEMENTS**

* allow overriding of PostgreSQL [shutdown timeout](https://pellepelster.github.io/solidblocks/hetzner/rds/#input\_postgres\_stop\_timeout)
* allow passing of [arbitrary environment](https://pellepelster.github.io/solidblocks/hetzner/rds/#input\_environment\_variables) variables to PostgreSQL

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