---
title: Software
weight: 40
disableToc: true
---

Tooling setup for local development and continuous integration environments.

Based on the functionality from [Download]({{%relref "download/_index.md" %}}) and [File]({{%relref "download/_index.md" %}}) this library offers automatic installation for various software packages.

The packages will be downloaded to a local `.cache` directory, extracted to `.bin` directory and executed from there. This helps to avoid polluting the system with software only needed for your project and also avoids version conflicts in case other projects use different versions of the same tool.

## Usage

Use the `software_ensure_*` to install the needed software and then it to the search `$PATH` via `software_set_export_path`

```shell
source "software.sh"

software_ensure_shellcheck
software_set_export_path

shellcheck -v
```

## Overriding .cache and .bin directories

The `.cache` and `.bin` directory can be overwritten by setting `$CACHE_DIR` and `$BIN_DIR` before sourcing in the libraries. This can be useful in a continuous integration env to put the cache on a persistent storage to avoid re-downloading the software on every build. 

```shell
export BIN_DIR="/tmp/bin"
export CACHE_DIR="/tmp/cache"

source "software.sh"

software_ensure_shellcheck
```

## Functions

All functions can be called without any parameters and will then install the software in a recent version. An exact version and checksum for the downloaded can also be given to force a specific version.

```shell
source "software.sh"

software_ensure_terraform

software_ensure_terraform "0.13.4" "a92df4a151d390144040de5d18351301e597d3fae3679a814ea57554f6aa9b24"
```

## `software_ensure_terraform(version = TERRAFORM_VERSION, checksum)` {#software_ensure_terraform}
Installs [HashiCorp Terraform](https://www.terraform.io/) version TERRAFORM_VERSION

## `software_ensure_consul(version = CONSUL_VERSION, checksum)` {#software_ensure_consul}
Installs [HashiCorp Consul](https://www.consul.io/) version CONSUL_VERSION

## `software_ensure_hugo(version = HUGO_VERSION, checksum)` {#software_ensure_hugo}
Installs [Hugo](https://gohugo.io/) static site generator version HUGO_VERSION

## `software_ensure_shellcheck(version = SHELLCHECK_VERSION, checksum)` {#software_ensure_shellcheck}
Installs [ShellCheck](https://www.shellcheck.net/) shell script analysis tool version SHELLCHECK_VERSION

## `software_ensure_semver(version = SEMVER_VERSION, checksum)`  {#software_ensure_semver}
Installs [semver](https://github.com/maykonlf/semver-cli) a semantic versioning tool in version SEMVER_VERSION


## `software_hashicorp_ensure(product, version, checksum)` {#software_hashicorp_ensure}
Generic wrapper for downloading HashiCorp tools built around the convention that product distributions are available at https://releases.hashicorp.com/`${product}`/`${version}`/`${product}`_`${product}`_linux_amd64.zip and the downloaded
zip contains an executable named `${product}` which will be written to `${bin_dir}`.

```shell
software_hashicorp_ensure "nomad" "1.3.3" "d908811cebe2a8373e93c4ad3d09af5c706241878ff3f21ee0f182b4ecb571f2"
```

## `software_export_path` {#software_export_path}
Creates a `$PATH` compatible path for all software downloaded with `software_ensure_*`

```shell
source "software.sh"

software_ensure_shellcheck

export PATH="${PATH}:$(software_export_path)"

shellcheck --version
```

## `software_set_export_path` {#software_set_export_path}
Updates `$PATH` to include all software downloaded with `software_ensure_*`

```shell
source "software.sh"

software_ensure_hugo

software_set_export_path

hugo version
```