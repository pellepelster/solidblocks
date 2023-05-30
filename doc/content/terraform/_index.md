---
title: Terraform
weight: 60
description: Helpers to bootstrap terraform storage backends
---

Starting a new Terraform managed IaC code project comes with its own chicken-or-egg problem. Terraform needs a backend to store its state, this creates a bootstrapping problem because it is by design not capable to configure its own backend (because it can't store the state required to provision the state backend).

## State Backends

Terraform supports a wide [range of backends](https://developer.hashicorp.com/terraform/language/settings/backends/configuration#available-backends), the backend commands will provide support for setting up those backends.
If applicable the commands will try to resolve configuration drift and ensure the state backend resources conform to Terraforms expected configuration.

### AWS S3

This command will set up a S3 bucket for state storage and a DynamoDB table for state locking according to [Terraform S3 backend](https://developer.hashicorp.com/terraform/language/settings/backends/s3). It will also make sure versioning is enabled and that the bucket is private only. 


Create a new S3 state backend by running:

```shell
docker run \
    -e AWS_REGION="eu-central-1" \
    -e AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}" \
    -e AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}" \
    ghcr.io/pellepelster/solidblocks-terraform:snapshot \
    backend s3 "${bucket-name}"
```

final output will be a Terraform state backend config using the created resources

```
creating bucket 'my-terraform-state-bucket-123'
setting public access block config for bucket 'my-terraform-state-bucket-123'
enabling versioning for bucket 'my-terraform-state-bucket-123'
creating table 'my-terraform-state-bucket-123'

example configuration for created backend

terraform {
  backend "s3" {
    region          = "eu-central-1"
    bucket          = "my-terraform-state-bucket-123"
    dynamodb_table  = "my-terraform-state-bucket-123"
    key             = "<some_key>"
  }
}
```

