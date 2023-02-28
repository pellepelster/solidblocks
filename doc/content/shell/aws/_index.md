---
title: AWS
weight: 60
disableToc: true
---

Utilities for the AWS cloud API

## Functions

### `aws_dynamodb_ensure(table_name, attribute_definitions, key_schema)` {#aws_dynamodb_ensure}

Creates a new DynamoDB table named `table_name` if it not already exists. The command will wait until the table is completely provisioned and ready to use. The parameters `attribute_definitions` and `key_schema` need to be provided according to the schema documented in the AWS DynamoDB [create-table](https://awscli.amazonaws.com/v2/documentation/api/latest/reference/dynamodb/create-table.html) documentation.

```shell
source "aws.sh"

aws_dynamodb_ensure "my-table" "AttributeName=attribute1,AttributeType=S" "AttributeName=attribute1,KeyType=HASH"
```

### `aws_bucket_ensure(bucket_name, [location=eu-west-1])` {#aws_bucket_ensure}

Creates a new S3 bucket named `bucket_name` if it not already exists. The command will fail if bucket creation fails.

```shell
source "aws.sh"

aws_bucket_ensure "my-bucket"
```