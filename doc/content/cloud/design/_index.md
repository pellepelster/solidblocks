+++
title = 'Design'
+++

The overall design of Solidblocks cloud is led by the following rules

* Only basic building blocks that are widely available across the majority of cloud providers are used, e.g. VM, storage disks, DNS and private networking to keep the setup simple and portable
* Instead of complex container schedulers or API driven control-planes Solidblocks clouds relies on plain Unix services and docker containers started with systemd
* No extra or intermediary state is used, the source of truth is the configuration file. Resources are identified solely based on its name
* Apart from the data and backup disks, every created resource must be treated as ephemeral. It must always be possible to re-provision from scratch and get the same system-state as before
* The created VMs can survive standalone, it must always be possible to use them without Solidblocks cloud
* It is open for integration of resources that are managed out of band with other IaC tools
* It optimizes for MTTR over MTBF, instead of complicated failover or rolling update processes, short outages during service deployments are acceptable


## Overview

Solidblocks cloud uses a cyclic process starting with a high level `configuration` that is transformed into a runtime `model` defining all resources that are needed to implement the services. During the `plan` phase this model is compared with the running state inside the cloud, and a `diff` is created containing all missing or changed resources. Based on this `diff` during the `apply` phase the running state is diverged towards the intended `model` derived from the `configuration`.

![Provisioning Cycle](provisioning_cycle.excalidraw.png)


## Provisioning Process

The provisioning process is divided into multiple steps

![Provisioning Process](../provisioning_process.excalidraw.png)

#### Configuration to model

The configuration is read and transformed into an internal model. This model expands the configuration into the different infrastructure components that are needed to fulfill the requested service configuration. E.g. a service of the type `postgres` is built from:
 * a VM running the PostgreSQL database
 * a disk holding the PostgreSQL data
 * a backup disk holding the database backups
 * a secret for the PostgreSQL admin user
 * a DNS entry pointing to the database
 * a firewall rule restricting access to the database
 * ...

#### Running state to diff

The infrastructure resources from the created model that should be running are compared with the currently running state. From this comparison a diff is created containing the resources that need to be created or modified.

#### Diff to Resources

The changed resources from the diff are then created or modified to achieve the desired state from the model.


### Provisioning Methods


![Provisioning Methods](../provisioning_methods.excalidraw.png)

Different methodologies are used to provision services. 

#### ⓿ Infrastructure API calls

At the lowest layer, cloud resources are created using the public API of the underlying cloud provider.

#### ❶ Cloud-Init

The created machines are started using a service specific cloud-init script that will bootstrap the desired service.

#### ❷ VM Management

If needed VM management tasks are executed over SSH. This could be service start/restart, system updates, file provisioning etc.

#### ❸ Service Configuration

When the service is started and if needed further configuration is done on the services API using an SSH tunnel to prevent potential sensitive APIs from being exposed on the internet. This could for example be the creation of users and schemas on a database. 

### Resource Identity
The model and the created resources are linked using a predictable resource names. E.g. for the cloud named `cloud1` and the service `webservice1` the resource name for the virtual machine will be `cloud1-default-webservice1-0` following the pattern `<cloud_name>-<envrionment_name>-<service_name>-<index>`

{{% notice info %}}
Environment (`<envrionment_name>`) and multiple instance support (`<index>`) are not yet implemented, but already incorporated in the naming scheme to allow for a smooth transition in the future, see roadmap.
{{% /notice %}}


### Providers

Providers enable Solidblocks cloud to create all the needed resources like virtual machines, storage volumes, DNS entries or secrets to implement a service. For a minimal cloud configuration at three different provider types are needed:

* **SSH key provider**
  Used to load SSH keys that are used for cloud VM management.

* **Secret Provider** To provision and manage services, secrets are needed for API keys, database users, etc. The secret provider is used to store and retrieve secrets by a secret path.

* **Cloud Provider** The cloud provider implements the creation of the needed cloud resources like virtual machines, storage volumes, firewall, etc.

### Services

Services are created using high level service definitions. For example given a service with the type `postgresql` will instruct Solidblocks cloud to create a VM with a data and backup disk, install PostgreSQL, setup backup and recovery and create all needed database users.
