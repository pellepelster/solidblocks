+++
title = 'Hetzner Nuke'
description = 'Hetzner Nuke deletes all resources in a Hetzner account, similar to aws-nuke'
aliases = ['/hetzner/nuke']
overviewDescription = 'Hetzner Nuke deletes all resources in a Hetzner account'
faIcon = "fa-bomb"
+++

`blcks hetzner nuke` deletes all resources in a Hetzner account, similar to [aws-nuke](https://github.com/rebuy-de/aws-nuke).

**Use Cases**

* Clean up projects provisioned from CI/CD systems after build failures
* Ensure clean accounts for testing bootstrap capability of IaC code
* Save costs by cleaning development accounts overnight

{{% notice warning %}}
Be aware that `blcks hetzner nuke` is a very destructive operation, hence you have to be very careful while using it. Otherwise, you might delete production data.
{{% /notice %}}

## Usage

`blcks hetzner nuke` is available as a command in the [Solidblocks CLI](/cli). The Hetzner cloud API token can be provided as argument `--hetzner-token` or via environment variable `HCLOUD_TOKEN`.

### Simulate

When invoked without `--do-nuke` resource deletion will only be simulated, and the resources that would be deleted are printed to the console.

```shell
export HCLOUD_TOKEN=<hetzner api token>
blcks hetzner nuke
```

### Nuke

{{% notice warning %}}
`blcks hetzner nuke --do-nuke` mode will delete all resources that are reachable with the provided `HCLOUD_TOKEN`.
{{% /notice %}}

```shell
export HCLOUD_TOKEN=<hetzner api token>
blcks hetzner nuke --do-nuke
```