+++
title = "Terraform/Tofu"
description = "Helpers for Terraform/Tofu IaC projects"
+++

## Backends

Manage Terraform or Tofu state backends

### S3

Create a private S3 suitable for Terraform and Tofu state backends

```shell
blcks tofu backends s3 my-state-bucket
```

```
[blcks] creating S3 bucket 'my-state-bucket'
[blcks] public access for S3 bucket 'my-state-bucket' already disabled
[blcks] enabling versioning for bucket 'my-state-bucket'
[blcks] state configuration for created S3 bucket
---
terraform {
    backend "s3" {
        region          = "eu-central-1"
        bucket          = "my-state-bucket"
        key             = "main"
    }
}
---
```


The config can also directly be written to a file via

```shell
blcks tofu backends s3 --file /path/to/state-config.tf my-state-bucket
```
