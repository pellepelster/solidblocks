---
title: Development
weight: 99
description: Guide for development of Solidblocks Hetzner RDS
---

## Testing

Before test can be run the project needs to be built via

```bash
./do build
```

The integration tests make sure all major use cases are supported, all data is kept and use the following flow:

* create required resource prerequisites in separate terraform module
  * `terraform_wrapper "test/terraform/base" apply -auto-approve`
* create RDS instance
  * `terraform_wrapper "test/terraform/instance_s3" apply -auto-approve`
* wait for cloud init to finish (`test_wait_for_cloud_init`)
* wait for docker to come up (`test_wait_for_docker`)
* wait for PostgreSQL to start (`test_wait_for_sql`)
* verify machine setup an configuratio using testinfra tests `test_${test_case}.py`
* create test data
  * `create_table | psql_wrapper`
  * `insert_dog | psql_wrapper`
* destroy and recreate instance
* verify data is still available
  * `echo "SELECT * FROM dogs;" | psql_wrapper | grep "rudi"`

The test cases can be executed using the provided `do` tasks

* `./do test-restore-local`
* `./do test-restore-s3`
* `./do test-private-network`
* `./do test-migration`
* `./do test-encryption`
* `./do test-arm`
* `./do test-ssl`

or all together with

* `./do test`

In case of test failures the currently provisioned test machine can be reached via ssh with

```bash
./do test-ssh
```
