+++
title = 'Hetzner ASG'
description = 'A rolling rotate for cloud servers attached to a Hetzner load balancer'
overviewDescription = 'A rolling rotate for cloud servers attached to a Hetzner load balancer'
faIcon = "fa-cloud"
+++

## Overview

A rolling replace for cloud servers attached to a Hetzner load balancer. All servers that are 
attached to a Hetzner load balancer provided by `--loadbalancer` will be replaced with new 
servers created using the user data from `--user-data`.

## Details

The command works stateless in the sense that all needed information is stored as meta-data
in the labels of the affected Hetzner cloud resources. Only resources created by this command
are managed, the meta-data label `blcks.de/managed-by` set to **asg** serves as marker for those
resources.

The decision if a server needs to be replaced is made by comparing the SHA-256 hash of 
the user data script used to create a server with the hash of the new user data script 
provided by `--user-data`. The user data hash is stored in the `blcks.de/user-data-hash`
label of each server.

The rotate process is atomic and can be cancelled and restarted at any time. Once 
started it will try to reconcile towards the target state of *n* up-to-date `--replicas`
and afterward remove all old servers attached to the `--loadbalancer`.
 
If a server gets detached from a load balancer, it will automatically be re-attached. 
To achieve this, the load balancer association is stored in the 
`blcks.de/load-balancer-id` labels.

## Usage

**user-data.sh**
```
#!/usr/bin/env bash

apt-get update
apt-get -y install nginx
```

**replace all servers on load balancer **application1** with new servers created from `user-data.sh`**
```shell
blcks hetzner asg rotate --loadbalancer application1 --user-data user-data.sh --replicas 2
```