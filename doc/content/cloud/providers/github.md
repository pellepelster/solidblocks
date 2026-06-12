+++
title = 'github'
description = 'Provides integration of GitHub resources'
weight = 70
+++

The `github` provider provides integration of GitHub resources and is required by the [`github_runner`]({{% relref "../services/github-runner.md" %}}) service. The target is configured via `github_url`, either an organisation (`https://github.com/<org>`) or a repository (`https://github.com/<user>/<repo>`).

## Environment Variables

A personal access token with appropriate permissions must be supplied via the environment variable `GITHUB_TOKEN`.

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
```

See the [configuration format]({{% relref "../configuration/format.md" %}}) for the full keyword reference.
