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
  - type: <s3|postgresql|docker>
    #...

```
## Keywords
## name
*type*: **string**,
*optional*: **false**,
*min. length*: **2**,
*max. length*: **63**,
*default*: **\<none\>**

Unique name for the cloud deployment. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name. If you plan to deploy multiple Solidblocks cloud configurations to a single provider account make sure the names are unique across all configuration files.

## root_domain
*type*: **string**,
*optional*: **true**,
*min. length*: **4**,
*max. length*: **253**,
*default*: **\<none\>**

Root domain to use for addresses of created services, e.g. `<service_name>.<root_domain>`. If set the domain must be manageable by one of the configured providers.

# Providers

Provider list, if two providers of the same type are configured, unique names must be provided. For a minimal configuration at least a SSH, secret and cloud provider is needed.

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
*type*: **string**,
*optional*: **true**,
*default*: **default**

Name for the provider, can be omitted if only one provider of this specific type is configured

### default-location
*type*: **string**,
*optional*: **true**,
*options*: **fsn1, nbg1, hel1, ash, hil, sin**
*default*: **fsn1**

Default location for created infrastructure resources

### default-instance-type
*type*: **string**,
*optional*: **true**,
*options*: **cpx21, cpx31, cpx41, cpx51, cax11, cax21, cax31, cax41, ccx13, ccx23, ccx33, ccx43, ccx53, ccx63, cpx12, cpx22, cpx32, cpx42, cpx52, cpx62, cx23, cx33, cx43, cx53**
*default*: **cpx21**

Default instance size for virtual machines

## Pass

Stores secrets in the [pass](https://www.passwordstore.org/) secret manager. To ensure that the store is setup correctly a temporary secret will be created and deleted during the configuration validation phase.

```yaml
type: pass
password_store_dir: [string]

```
### Keywords
### password_store_dir
*type*: **string**,
*optional*: **true**,
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
*type*: **string**,
*optional*: **true**,
*default*: **\<none\>**

Path to the private key, if not set, the default SSH key paths will be tried ('~/.ssh/id_rsa', '~/.ssh/id_ecdsa', '~/.ssh/id_ecdsa_sk', '~/.ssh/id_ed25519', '~/.ssh/id_ed25519_sk')

# Services

Services to create, service names must be unique across all services

## S3

S3 compatible object storage service based on [GarageFS](https://garagehq.deuxfleurs.fr/). Currently only single region deployment are supported.

```yaml
type: s3
name: <string>
size: [number]
buckets:
  - name: <string>
    public_access: [boolean]
    access_keys:
      - name: <string>
        owner: [boolean]
        read: [boolean]
        write: [boolean]
        #...
    #...

```
### Keywords
### name
*type*: **string**,
*optional*: **false**,
*min. length*: **2**,
*max. length*: **63**,
*default*: **\<none\>**

Unique name for the service. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name.

### size
*type*: **number**,
*optional*: **true**,
*default*: **16**

Size in GB for the data volume

### buckets

List of S3 buckets to create. Buckets that are removed from this list will not be deleted automatically.

#### name
*type*: **string**,
*optional*: **false**,
*min. length*: **4**,
*max. length*: **253**,
*default*: **\<none\>**

Unique name for the bucket. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name.

#### public_access
*type*: **boolean**,
*optional*: **true**,
*default*: **false**

If enabled the bucket content will be publicly available via 'https' without any authentication

#### access_keys

Access keys to generate for bucket access

##### name
*type*: **string**,
*optional*: **false**,
*default*: **\<none\>**

Unique name for the access key

##### owner
*type*: **boolean**,
*optional*: **true**,
*default*: **false**

Grant owner permission to the access key

##### read
*type*: **boolean**,
*optional*: **true**,
*default*: **false**

Grant read permission to the access key

##### write
*type*: **boolean**,
*optional*: **true**,
*default*: **false**

Grant write permission to the access key

## PostgreSQL

Single node PostgreSQL database instance with pgBackRest powered backup.

```yaml
type: postgresql
name: <string>
size: [number]
databases:
  - name: <string>
    users:
      - name: <string>
        admin: [boolean]
        read: [boolean]
        write: [boolean]
        #...
    #...

```
### Keywords
### name
*type*: **string**,
*optional*: **false**,
*min. length*: **2**,
*max. length*: **63**,
*default*: **\<none\>**

Unique name for the service. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name.

### size
*type*: **number**,
*optional*: **true**,
*default*: **16**

Size in GB for the data volume

### databases

List of databases to create. Databases that are removed from this list will not be deleted automatically.

#### name
*type*: **string**,
*optional*: **false**,
*min. length*: **4**,
*max. length*: **253**,
*default*: **\<none\>**

Unique name for the bucket. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name.

#### users

Users to create for database access

##### name
*type*: **string**,
*optional*: **false**,
*default*: **\<none\>**

Unique name for the access key

##### admin
*type*: **boolean**,
*optional*: **true**,
*default*: **false**

Grant full DDL privileges to the user

##### read
*type*: **boolean**,
*optional*: **true**,
*default*: **false**

Grant read permissions to the user

##### write
*type*: **boolean**,
*optional*: **true**,
*default*: **false**

Grant update/insert and delete permissions to the user

## Docker

Deploys a docker service image containers and exposes its endpoints

```yaml
type: docker
name: <string>
endpoints:
  - port: [number]
    #...

```
### Keywords
### name
*type*: **string**,
*optional*: **false**,
*min. length*: **2**,
*max. length*: **63**,
*default*: **\<none\>**

Unique name for the service. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name.

### endpoints

Service endpoints to expose

#### port
*type*: **number**,
*optional*: **true**,
*default*: **\<none\>**

Unique name for the endpoint
