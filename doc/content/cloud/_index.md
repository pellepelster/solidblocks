+++
title = 'Cloud'
description = 'Locally managed cloud services on bare VMs'
overviewGroup = "cloud"
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
{{% include "/snippets/quickstart.yaml" %}}
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
{{< asciicast src="/cloud/casts/quickstart.cast" poster="npt:0:04" autoPlay=true loop=true >}}


## Features

## Simplicity

Solidblocks Cloud tries to avoid complexity in the deployed services where possible to make the deployed systems easier to understand, debug and maintain. All VMs are provisioned with Debian as underlying operating systems and SystemD as service manager. Management happens over SSH and that’s it. A human can step in at any time.    

## Secret handling

## Backup & Restore

## Help

* ssh access
* connect to db help

## Status

* show backup status
