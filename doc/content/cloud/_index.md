+++
title = 'Cloud'
description = 'Solidblocks Cloud is a standalone CLI tool that deploys managed services on bare virtual machines, networking and storage. No container schedulers, no cloud specific services, just plain virtual machines running services on Linux , managed via SSH.'
overviewGroup = "main"
faIcon = "fa-cloud"
aliases = ["cli/cloud/"]
+++

Solidblocks Cloud is a standalone CLI tool that deploys managed services on bare virtual machines, networking and storage. No container schedulers, no cloud specific services, just plain virtual machines running services on Linux , managed via SSH.

## Installation
Solidblocks Cloud is part of the Solidblocks CLI, look [here](cli/) for installation instructions. 

## Quickstart

**generate SSH key**

```shell
ssh-keygen -t ed25519 -f cloud1.key -q -N ""
````

**create configuration `cloud1.yaml`**  
```yaml
{{% include-safe "/snippets/quickstart.yaml" %}}
```

**apply configuration**
```shell
blcks cloud apply cloud1.yaml 
```

**access the deployed service**
```shell
export ENDPOINT=$(blcks cloud info cloud1.yaml --format json | jq -r '.services[] | select(.name == "service1").endpoints | first | .url')
curl ${ENDPOINT}/hello
```

**see it in action**
{{< asciicast src="cloud/casts/quickstart.cast" >}}


## Features

## Simplicity

Solidblocks Cloud is designed to avoid complexity in the deployed services where possible, to make the deployed virtual machines easier to understand, debug and maintain. All VMs are provisioned with Debian as underlying operating systems and SystemD as service manager. Management happens over SSH and that’s it. A human can step in at any time.    

## Secret handling

Strong credentials are generated for all deployed services and automatically added to the configured secret provider backend. Rotating a password becomes as easy as updating the secret and triggering a new apply.

{{< asciicast src="cloud/casts/quickstart_secrets.cast" >}}


## DNS and Certificates

If the configured cloud provider supports DNS, switching to DNS based endpoints secured by SSL certificates instead of IP addresses is as easy as adding the `root_domain` to the config file.

{{< asciicast src="cloud/casts/quickstart_dns.cast" >}}


## Backup & Restore

To protect your data, Solidblocks includes automatic backup and restore functionality. Besides backup to locally attached disks backups can also be written to AWS S3 buckets, allowing for a complete disaster recovery of the system state from only S3. 

{{< asciicast src="cloud/casts/quickstart_s3.cast" >}}

## Help

Context-sensitive information and error messages help connecting to the deployed services.

{{< asciicast src="cloud/casts/quickstart_help.cast" >}}
