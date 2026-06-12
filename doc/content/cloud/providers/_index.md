+++
title = 'Providers'
weight = 25
+++

Providers supply the infrastructure, SSH, secret, backup and GitHub integrations that the deployed services build on. They are declared in the `providers` list of the configuration file, where each entry selects a provider `type`

```yaml
providers:
  - type: <hcloud|protonpass|pass|ssh_key|backup_aws_s3|backup_local|github>
    #...
```

Exactly one `ssh_key` provider is required, all other categories allow at most one provider: a cloud provider for infrastructure resources, a secret provider for generated credentials, a backup provider for service backups and a GitHub provider for GitHub integrations. See the [configuration format]({{% relref "../configuration/format.md" %}}) for the full keyword reference.

## Cloud

### [`hcloud`]({{% relref "hcloud.md" %}})

Provides Hetzner Cloud based infrastructure resources.

## SSH

### [`ssh_key`]({{% relref "ssh-key.md" %}})

Loads local file based SSH keys used to manage the provisioned virtual machines.

## Secrets

### [`pass`]({{% relref "pass.md" %}})

Stores secrets in the [pass](https://www.passwordstore.org/) password manager.

### [`protonpass`]({{% relref "protonpass.md" %}})

Stores secrets in a [Proton Pass](https://proton.me/pass) vault.

## Backup

### [`backup_local`]({{% relref "backup-local.md" %}})

Enables use of locally attached disks for service backups.

### [`backup_aws_s3`]({{% relref "backup-aws-s3.md" %}})

Uses AWS S3 buckets for service backups.

## GitHub

### [`github`]({{% relref "github.md" %}})

Provides integration of GitHub resources.
