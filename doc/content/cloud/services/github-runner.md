+++
title = 'github_runner'
description = 'Provisions self-hosted GitHub Actions runners based on the configured cloud provider'
weight = 40
+++

The `github_runner` service provisions self-hosted GitHub Actions runners based on the configured cloud provider. The number of runner instances is controlled via `scale` (0 to 10, default 1).

Workflow jobs can be routed to the runners via the `labels` list, extra Ubuntu packages needed by the workflows can be installed during machine provisioning via `packages`. With `allow_sudo` enabled the GitHub runner user can run password-less sudo commands.

## Required Providers

A cloud provider (`hcloud`) and a `github` provider configured with the target organisation or repository URL and a personal access token supplied via the `GITHUB_TOKEN` environment variable.

## Example

```yaml
name: cloud1

providers:
  - type: ssh_key
  - type: hcloud
  - type: github
    github_url: https://github.com/pellepelster/solidblocks

services:
  - type: github_runner
    name: runner1
    scale: 2
    labels:
      - linux
      - docker
    packages:
      - build-essential
```

See the [configuration format]({{% relref "../configuration/format.md" %}}) for the full keyword reference.
