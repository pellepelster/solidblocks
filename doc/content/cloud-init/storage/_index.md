---
title: Storage
weight: 10
disableToc: true
---

Utilities for storage handling

## Functions

### `aws_dynamodb_ensure(table_name, attribute_definitions, key_schema)` {#aws_dynamodb_ensure}

Creates a new DynamoDB table named `table_name` if it not already exists. The command will wait until the table is completely provisioned and ready to use. The parameters `attribute_definitions` and `key_schema` need to be provided according to the schema documented in the AWS DynamoDB [create-table](https://awscli.amazonaws.com/v2/documentation/api/latest/reference/dynamodb/create-table.html) documentation.

```shell
source "aws.sh"

aws_dynamodb_ensure "my-table" "AttributeName=attribute1,AttributeType=S" "AttributeName=attribute1,KeyType=HASH"
```

