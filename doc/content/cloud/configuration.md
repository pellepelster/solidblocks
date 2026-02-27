+++
title = 'Configuration'
description = 'configuration file format documentation'
+++

A Solidblocks instance can be defined using a YAML based configuration file with the following format

```yaml
name: <string>
root_domain: [string]

providers:
  - type: <hcloud|pass|ssh_key>
    #...

services:
  - type: <s3>
    #...

```
## Keywords
## name
*type*: **string**

*optional*: **false**

*default*: **\<none\>**


Unique name for the cloud deployment. Can be up to 63 characters long and must adhere to [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name. If you plan to deploy multiple Solidblocks cloud configurations to a single provider account make sure the names are unique across all configuration files.

## root_domain
*type*: **string**

*optional*: **true**

*default*: **\<none\>**


Root domain to use for addresses of created services, e.g. `\<service_name\>.\<root_domain\>`. If set the domain must be manageable by one of the configured providers.

# providers
## Hetzner

Provides Hetzner Cloud based infrastructure resources. An API key with read/write access must be provided via the environment variable `HCLOUD_TOKEN`.

```yaml
type: hcloud
name: [string]
default-location: [string]
default-instance-type: [string]

```
### Keywords
### name
*type*: **string**

*optional*: **true**

*default*: **default**


Name for the provider, can be omitted if only one provider of this specific type is configured

### default-location
*type*: **string**

*optional*: **true**

*default*: **nbg1**


Default location for created infrastructure resources

### default-instance-type
*type*: **string**

*optional*: **true**

*default*: **cx23**


Default instance size for virtual machines

## Pass

Stores secrets in the [pass](https://www.passwordstore.org/) secret manager. To ensure that the store is setup correctly a temporary secret will be created and deleted during the configuration validation phase.

```yaml
type: pass
password_store_dir: [string]

```
### Keywords
### password_store_dir
*type*: **string**

*optional*: **true**

*default*: **\<none\>**


Storage path for the password store, if not set the default or the setting from the `PASSWORD_STORE_DIR` environment variable will be used.

## Local SSH

A provider that loads local file based SSH keys. It supports passwordless PEM as well as OpenSSH encoded private keys.

```yaml
type: ssh_key
private_key: [string]

```
### Keywords
### private_key
*type*: **string**

*optional*: **true**

*default*: **\<none\>**


Path to the private key, if not set, the default SSH key paths will be tried ('\~/.ssh/id_rsa', '\~/.ssh/id_ecdsa', '\~/.ssh/id_ecdsa_sk', '\~/.ssh/id_ed25519', '\~/.ssh/id_ed25519_sk')

# services
## S3

S3 compatible object storage service based on [GarageFS](https://garagehq.deuxfleurs.fr/).

```yaml
type: s3
name: <string>
buckets:
  name: <string>
  public_access: [boolean]

```
### Keywords
### name
*type*: **string**

*optional*: **false**

*default*: **\<none\>**


TODO
