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
{{% include "/snippets/cloud-minimial-example.yml" %}}
```

**apply configuration**
```shell
blcks cloud apply cloud1.yaml 
```

**access the deployed service**
```shell
curl http://<ip adress>/hello
```

**see it in action**
{{< asciicast src="/cloud/casts/cloud1.cast" poster="npt:0:04" autoPlay=true loop=true >}}


### Configuration

The configuration file is the central source of truth for all deployments. It is written in YAML and defines the services to deploy and where to deploy them

**example**
```yaml
{{% include "/snippets/cloud-minimial-example.yml" %}}
```

To see what the model looks like and which resources would be created/changed by an apply, run

```shell
blcks cloud plan <configuration>
```

which will give you a detailed overview over the pending changes. To apply those changes run

```shell
blcks cloud apply <configuration>
```

which will deploy the changes, and provide you with an overview about the deployed services afterward. A full description of all configuration options is available online [here](./configuration) or via the CLI

```shell
blcks cloud help configuration
```

### Providers

Providers enable Solidblocks cloud to create all the needed resources like virtual machines, storage volumes, DNS entries or secrets to implement a service. For a minimal cloud configuration at three different provider types are needed:

* **SSH key provider**
Used to load SSH keys that are used for cloud VM management. 

* **Secret Provider** To provision and manage services, secrets are needed for API keys, database users, etc. The secret provider is used to store and retrieve secrets by a secret path.

* **Cloud Provider** The cloud provider implements the creation of the needed cloud resources like virtual machines, storage volumes, firewall, etc.

### Services

Services are created using high level service definitions. For example given a service with the type `postgresql` will instruct Solidblocks cloud to create a VM with a data and backup disk, install PostgreSQL, setup backup and recovery and create all needed database users.
