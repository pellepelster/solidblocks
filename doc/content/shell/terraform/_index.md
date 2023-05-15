---
title: Terraform
weight: 70
description: Wrappers and helpers for Terraform
---

Wrappers and helpers for Terraform

## Functions

### `terraform_aws_state_backend(name)` {#terraform_aws_state_backend}

Creates a new S3 Bucket and DynamoDB ready to be used as [Terraform state backend](https://developer.hashicorp.com/terraform/language/settings/backends/s3). The provided name is used as name for the bucket as well as for the DynamoDB table.

```shell
source "software.sh"
source "terraform.sh"

software_ensure_terraform
software_set_export_path

terraform_aws_state_backend "my-project"

terraform init -reconfigure \
  -backend-config="bucket=my-project" \
  -backend-config="dynamodb_table=my-project"

terraform apply -auto-approve
```

**Terraform**
```terraform
terraform {
  backend "s3" {
    key = "my-project/terraform.state"
  }
}

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "4.56.0"
    }
  }
}

provider "aws" {
}

resource "random_uuid" "test_id" {}

resource "aws_s3_bucket" "bucket" {
  bucket = "test-${random_uuid.test_id.id}"
}
```