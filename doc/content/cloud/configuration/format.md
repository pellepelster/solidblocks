+++
title = 'Format'
description = 'configuration file format documentation'
+++

A Solidblocks instance is defined using a YAML based configuration file with the following format

```
name: <string>
environment_vars: [map]
root_domain: [string]

providers:
  - type: <hcloud|pass|ssh_key|backup_aws_s3|backup_local|github>
    #...

services:
  - type: <s3|postgresql|docker|github_runner>
    #...
```
## Keywords
## name
*type*: **string**, *optional*: **false**, 
*min. length*: **2**, *max. length*: **63**, *default*: **&lt;none&gt;**

Unique name for the cloud deployment. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name. If you plan to deploy multiple Solidblocks cloud configurations to a single provider account make sure the names are unique across all configuration files.

## environment_vars
*type*: **map**, *optional*: **true**, 
*default*: **&lt;none&gt;**

Environment variables that should be set for all services, can be overridden on a per-service basis

## root_domain
*type*: **string**, *optional*: **true**, 
*min. length*: **4**, *max. length*: **253**, *default*: **&lt;none&gt;**

Root domain to use for addresses of created services, e.g. **&lt;service_name&gt;.&lt;root_domain&gt;**. If set, the domain must be manageable by one of the configured providers.

# Providers

Provider list, if two providers of the same type are configured, unique names must be provided. For a minimal configuration at least a SSH, secret and cloud provider is needed.

## Hetzner

Provides Hetzner Cloud based infrastructure resources. An API key with read/write access must be provided via the environment variable `HCLOUD_TOKEN`.

type: hcloud
```
name: [string]
default_location: [string]
default-instance-type: [string]
```
### Keywords
### name
*type*: **string**, *optional*: **true**, 
*default*: **default**

Name for the provider, can be omitted if only one provider of this specific type is configured

### default_location
*type*: **string**, *optional*: **true**, 
*options*: **fsn1, nbg1, hel1, ash, hil, sin**, *default*: **fsn1**

Default location for created infrastructure resources

### default-instance-type
*type*: **string**, *optional*: **true**, 
*options*: **cx23, cx33, cx43, cx53, cpx21, cpx31, cpx41, cpx51, cax11, cax21, cax31, cax41, ccx13, ccx23, ccx33, ccx43, ccx53, ccx63, cpx12, cpx22, cpx32, cpx42, cpx52, cpx62**, *default*: **cx23**

Default instance size for virtual machines

## Pass

Stores secrets in the [pass](https://www.passwordstore.org/) password manager. To ensure that the store is setup correctly a temporary secret will be created and deleted during the configuration validation phase. The validation can be skipped by setting the environment variable 'BLCKS_PASS_PROVIDER_SKIP_VALIDATION'

type: pass
```
name: [string]
password_store_dir: [string]
```
### Keywords
### name
*type*: **string**, *optional*: **true**, 
*default*: **default**

Name for the provider, can be omitted if only one provider of this specific type is configured

### password_store_dir
*type*: **string**, *optional*: **true**, 
*default*: **&lt;none&gt;**

Storage path for the password store, if not set the default or the setting from the `PASSWORD_STORE_DIR` environment variable will be used.

## Local SSH

A provider that loads local file based SSH keys. It supports passwordless PEM as well as OpenSSH encoded private keys.

type: ssh_key
```
private_key: [string]
```
### Keywords
### private_key
*type*: **string**, *optional*: **true**, 
*default*: **&lt;none&gt;**

Path to the private key, if not set, the default SSH key paths will be tried ('&tilde;/.ssh/id_rsa', '&tilde;/.ssh/id_ecdsa', '&tilde;/.ssh/id_ecdsa_sk', '&tilde;/.ssh/id_ed25519', '&tilde;/.ssh/id_ed25519_sk')

## Backup S3

Uses AWS S3 buckets for service backups. During plan/apply the `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` must be set with credentials that have the permission to create new S3 Buckets, as well as IAM users and access keys. For each service a dedicated backup bucket and separate IAM credentials will be created.

type: backup_aws_s3
```
name: [string]
region: [string]
```
### Keywords
### name
*type*: **string**, *optional*: **true**, 
*default*: **default**

Name for the provider, can be omitted if only one provider of this specific type is configured

### region
*type*: **string**, *optional*: **true**, 
*default*: **eu-central-1**

Region where the backup bucket should be created

## Backup Local

Enables use of locally attached disks for service backups.

type: backup_local
```
name: [string]
```
### Keywords
### name
*type*: **string**, *optional*: **true**, 
*default*: **default**

Name for the provider, can be omitted if only one provider of this specific type is configured

## GitHub

Provides integration of GitHub resources. A personal access token with appropriate permissions must be supplied via the environment variable `GITHUB_TOKEN`.

type: github
```
name: [string]
github_url: <string>
```
### Keywords
### name
*type*: **string**, *optional*: **true**, 
*default*: **default**

Name for the provider, can be omitted if only one provider of this specific type is configured

### github_url
*type*: **string**, *optional*: **false**, 
*default*: **&lt;none&gt;**

GitHub URL, either an organisation (https://github.com/&lt;org&gt;) or a repository (https://github.com/&lt;user&gt;/&lt;repo&gt;)

# Services

Services to create, service names must be unique across all services

## S3

S3 compatible object storage service based on [GarageFS](https://garagehq.deuxfleurs.fr/). Currently only single region deployment are supported.

type: s3
```
backup_size: [number]
backup_full_retention_days: [number]
name: <string>
environment_vars: [map]
use_floating_ip: [boolean]
data_size: [number]
hetzner_location: [string]
hetzner_instance_type: [string]
buckets:
  - name: <string>
    public_access: [boolean]
    access_keys:
      - name: <string>
        owner: [boolean]
        read: [boolean]
        write: [boolean]
        #...
    public_access_domains:
        #...
    #...
```
### Keywords
### backup_size
*type*: **number**, *optional*: **true**, 
*default*: **&lt;none&gt;**

Size in GB for the local backup volume. If not set the size will be derived from the data volume size and the amount of full backup retention days.

### backup_full_retention_days
*type*: **number**, *optional*: **true**, 
*default*: **7**

amount of days to keep full backups

### name
*type*: **string**, *optional*: **false**, 
*min. length*: **2**, *max. length*: **63**, *default*: **&lt;none&gt;**

Unique name for the service. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name.

### environment_vars
*type*: **map**, *optional*: **true**, 
*default*: **&lt;none&gt;**

Environment variables that should be set for the service, will be merged with the globally configured environment variables

### use_floating_ip
*type*: **boolean**, *optional*: **true**, 
*default*: **false**

use floating ip for public server access

### data_size
*type*: **number**, *optional*: **true**, 
*default*: **16**

Size in GB for the data volume keeping all data needed for this service.

### hetzner_location
*type*: **string**, *optional*: **true**, 
*options*: **fsn1, nbg1, hel1, ash, hil, sin**, *default*: **fsn1**

Hetzner location for created infrastructure resources, if not set the default from the Hetzner provider configuration is used.

### hetzner_instance_type
*type*: **string**, *optional*: **true**, 
*options*: **cx23, cx33, cx43, cx53, cpx21, cpx31, cpx41, cpx51, cax11, cax21, cax31, cax41, ccx13, ccx23, ccx33, ccx43, ccx53, ccx63, cpx12, cpx22, cpx32, cpx42, cpx52, cpx62**, *default*: **cx23**

Hetzner instance size for virtual machines, if not set the default from the Hetzner provider configuration is used.

### buckets

List of S3 buckets to create. Buckets that are removed from this list will not be deleted automatically.

#### name
*type*: **string**, *optional*: **false**, 
*min. length*: **4**, *max. length*: **253**, *default*: **&lt;none&gt;**

Unique name for the bucket. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name.

#### public_access
*type*: **boolean**, *optional*: **true**, 
*default*: **false**

If enabled the bucket content will be publicly available via 'https' without any authentication

#### access_keys

Access keys to generate for bucket access

##### name
*type*: **string**, *optional*: **false**, 
*default*: **&lt;none&gt;**

Unique name for the access key

##### owner
*type*: **boolean**, *optional*: **true**, 
*default*: **false**

Grant owner permission to the access key

##### read
*type*: **boolean**, *optional*: **true**, 
*default*: **false**

Grant read permission to the access key

##### write
*type*: **boolean**, *optional*: **true**, 
*default*: **false**

Grant write permission to the access key

#### public_access_domains

If 'public_access' is enabled the bucket will also listen on these Domains. Requires A/AAAA entries to point to the server hosting the buckets. If any provider supports those domains the entries will automatically be created.


## PostgreSQL

Single node PostgreSQL database instance with pgBackRest powered backup.

type: postgresql
```
majorVersion: [number]
backup_size: [number]
backup_full_retention_days: [number]
name: <string>
environment_vars: [map]
use_floating_ip: [boolean]
data_size: [number]
hetzner_location: [string]
hetzner_instance_type: [string]
databases:
  - name: <string>
    users:
      - name: <string>
        #...
    #...
```
### Keywords
### majorVersion
*type*: **number**, *optional*: **true**, 
*default*: **17**

Postgres major version

### backup_size
*type*: **number**, *optional*: **true**, 
*default*: **&lt;none&gt;**

Size in GB for the local backup volume. If not set the size will be derived from the data volume size and the amount of full backup retention days.

### backup_full_retention_days
*type*: **number**, *optional*: **true**, 
*default*: **7**

amount of days to keep full backups

### name
*type*: **string**, *optional*: **false**, 
*min. length*: **2**, *max. length*: **63**, *default*: **&lt;none&gt;**

Unique name for the service. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name.

### environment_vars
*type*: **map**, *optional*: **true**, 
*default*: **&lt;none&gt;**

Environment variables that should be set for the service, will be merged with the globally configured environment variables

### use_floating_ip
*type*: **boolean**, *optional*: **true**, 
*default*: **false**

use floating ip for public server access

### data_size
*type*: **number**, *optional*: **true**, 
*default*: **16**

Size in GB for the data volume keeping all data needed for this service.

### hetzner_location
*type*: **string**, *optional*: **true**, 
*options*: **fsn1, nbg1, hel1, ash, hil, sin**, *default*: **fsn1**

Hetzner location for created infrastructure resources, if not set the default from the Hetzner provider configuration is used.

### hetzner_instance_type
*type*: **string**, *optional*: **true**, 
*options*: **cx23, cx33, cx43, cx53, cpx21, cpx31, cpx41, cpx51, cax11, cax21, cax31, cax41, ccx13, ccx23, ccx33, ccx43, ccx53, ccx63, cpx12, cpx22, cpx32, cpx42, cpx52, cpx62**, *default*: **cx23**

Hetzner instance size for virtual machines, if not set the default from the Hetzner provider configuration is used.

### databases

List of databases to create. Databases that are removed from this list will not be deleted automatically.

#### name
*type*: **string**, *optional*: **false**, 
*min. length*: **4**, *max. length*: **253**, *default*: **&lt;none&gt;**

Unique name for the database

#### users

Users to create for database access

##### name
*type*: **string**, *optional*: **false**, 
*default*: **&lt;none&gt;**

Unique name for the access key

## Docker

Deploys a dockerized service exposes its endpoints.

type: docker
```
image: <string>
backup_size: [number]
backup_full_retention_days: [number]
name: <string>
environment_vars: [map]
use_floating_ip: [boolean]
data_size: [number]
hetzner_location: [string]
hetzner_instance_type: [string]
endpoints:
  - container_port: [number]
    type: [string]
    #...
links:
    #...
```
### Keywords
### image
*type*: **string**, *optional*: **false**, 
*default*: **&lt;none&gt;**

Docker image to deploy

### backup_size
*type*: **number**, *optional*: **true**, 
*default*: **&lt;none&gt;**

Size in GB for the local backup volume. If not set the size will be derived from the data volume size and the amount of full backup retention days.

### backup_full_retention_days
*type*: **number**, *optional*: **true**, 
*default*: **7**

amount of days to keep full backups

### name
*type*: **string**, *optional*: **false**, 
*min. length*: **2**, *max. length*: **63**, *default*: **&lt;none&gt;**

Unique name for the service. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name.

### environment_vars
*type*: **map**, *optional*: **true**, 
*default*: **&lt;none&gt;**

Environment variables that should be set for the service, will be merged with the globally configured environment variables

### use_floating_ip
*type*: **boolean**, *optional*: **true**, 
*default*: **false**

use floating ip for public server access

### data_size
*type*: **number**, *optional*: **true**, 
*default*: **16**

Size in GB for the data volume keeping all data needed for this service.

### hetzner_location
*type*: **string**, *optional*: **true**, 
*options*: **fsn1, nbg1, hel1, ash, hil, sin**, *default*: **fsn1**

Hetzner location for created infrastructure resources, if not set the default from the Hetzner provider configuration is used.

### hetzner_instance_type
*type*: **string**, *optional*: **true**, 
*options*: **cx23, cx33, cx43, cx53, cpx21, cpx31, cpx41, cpx51, cax11, cax21, cax31, cax41, ccx13, ccx23, ccx33, ccx43, ccx53, ccx63, cpx12, cpx22, cpx32, cpx42, cpx52, cpx62**, *default*: **cx23**

Hetzner instance size for virtual machines, if not set the default from the Hetzner provider configuration is used.

### endpoints

Service endpoints to publicly expose

#### container_port
*type*: **number**, *optional*: **true**, 
*default*: **&lt;none&gt;**

Service port on the docker container

#### type
*type*: **string**, *optional*: **true**, 
*options*: **http**, *default*: **http**

Type of the service endpoints. Endpoints with the type `http` are automatically terminated with TLS if a `root_domain` is set.

### links

Linked services will automatically expose environment variables to the linked service, e.g. database credentials. To see which variables are available run the `info` command.


## GitHub Runner

Provisions a self-hosted runner based on the configured cloud provider. Requires a `github` provider to be configured with the target organisation or repository URL and a personal access token supplied via the `GITHUB_TOKEN` environment variable.

type: github_runner
```
scale: [number]
allow_sudo: [boolean]
name: <string>
environment_vars: [map]
use_floating_ip: [boolean]
data_size: [number]
hetzner_location: [string]
hetzner_instance_type: [string]
labels:
    #...
packages:
    #...
```
### Keywords
### scale
*type*: **number**, *optional*: **true**, 
*default*: **1**

Number if runner instances to create

### allow_sudo
*type*: **boolean**, *optional*: **true**, 
*default*: **false**

allow password-less sudo commands for the GitHub runner user

### name
*type*: **string**, *optional*: **false**, 
*min. length*: **2**, *max. length*: **63**, *default*: **&lt;none&gt;**

Unique name for the service. Must conform with [RFC 1123](https://datatracker.ietf.org/doc/html/rfc1123) to ensure it can be used as part of a domain name.

### environment_vars
*type*: **map**, *optional*: **true**, 
*default*: **&lt;none&gt;**

Environment variables that should be set for the service, will be merged with the globally configured environment variables

### use_floating_ip
*type*: **boolean**, *optional*: **true**, 
*default*: **false**

use floating ip for public server access

### data_size
*type*: **number**, *optional*: **true**, 
*default*: **16**

Size in GB for the data volume keeping all data needed for this service.

### hetzner_location
*type*: **string**, *optional*: **true**, 
*options*: **fsn1, nbg1, hel1, ash, hil, sin**, *default*: **fsn1**

Hetzner location for created infrastructure resources, if not set the default from the Hetzner provider configuration is used.

### hetzner_instance_type
*type*: **string**, *optional*: **true**, 
*options*: **cx23, cx33, cx43, cx53, cpx21, cpx31, cpx41, cpx51, cax11, cax21, cax31, cax41, ccx13, ccx23, ccx33, ccx43, ccx53, ccx63, cpx12, cpx22, cpx32, cpx42, cpx52, cpx62**, *default*: **cx23**

Hetzner instance size for virtual machines, if not set the default from the Hetzner provider configuration is used.

### labels

list of runner labels used to route workflow jobs to this runner


### packages

extra Ubuntu packages to install during machine provisioning

