---
title: Hetzner Nuke
weight: 60
description: Hetzner nuke is a tool to delete all resources in a Hetzner account, similar to aws-nuke
---

Hetzner nuke is a tool to delete all resources in a Hetzner account, similar to [aws-nuke](https://github.com/rebuy-de/aws-nuke).

**Use Cases**

* Clean up accounts provisioned from CI/CD systems after build failures
* Ensure clean accounts for testing bootstrap capability of IaC code
* Save costs by cleaning development accounts overnight

{{% notice warning %}}
Be aware that hetzner-nuke is a very destructive tool, hence you have to be very careful while using it. Otherwise, you might delete production data.
{{% /notice %}}

## Usage

Hetzner-nuke is available as a docker image and only needs a Hetzner Cloud API token, that can be provided as argument `--hetzner-token` or environment variable `HCLOUD_TOKEN`.

### Simulate

`simulate` will output all resources that will be deleted in `nuke`mode.

```
docker run -e HCLOUD_TOKEN="${HCLOUD_TOKEN}" ghcr.io/pellepelster/solidblocks-hetzner-nuke:__SOLIDBLOCKS_VERSION__ simulate
```

### Nuke

{{% notice warning %}}
`nuke` mode will delete all resources that are reachable with the provided `HCLOUD_TOKEN`. 
{{% /notice %}}


```
docker run -e HCLOUD_TOKEN="${HCLOUD_TOKEN}" ghcr.io/pellepelster/solidblocks-hetzner-nuke:__SOLIDBLOCKS_VERSION__ nuke
```
