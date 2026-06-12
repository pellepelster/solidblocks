+++
title = 'backup_aws_s3'
description = 'Uses AWS S3 buckets for service backups'
weight = 60
+++

The `backup_aws_s3` provider uses AWS S3 buckets for service backups, allowing for a complete disaster recovery of the system state from only S3. For each service a dedicated backup bucket and separate IAM credentials are created, the bucket region can be set via `region` (default `eu-central-1`).

## Environment Variables

During plan/apply `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` must be set with credentials that have the permission to create new S3 buckets, as well as IAM users and access keys.

## Example

```yaml
name: cloud1

providers:
  - type: pass
  - type: ssh_key
  - type: hcloud
  - type: backup_aws_s3
    region: eu-central-1

services:
  #...
```

See the [configuration format]({{% relref "../configuration/format.md" %}}) for the full keyword reference.
