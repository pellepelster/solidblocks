---
title: Development
weight: 99
description: Guide for development of Solidblocks Hetzner
---

## Testing

Before test can be run the project needs to be built via

```bash
./do build
```

The two main integration tests `test_restore_from_s3` and `test_restore_from_local` ensure that the database can always
be recovered from the selected backup repository (S3 or local storage) are orchestrated via the `do` file.

Test can be run all together via

```bash
./do test
```

or individually

```bash
./do test-restore-[local|s3]
```

After machine setup and before each test is started a [testinfra](https://testinfra.readthedocs.io/en/latest/) based test is started that can be used to assert the state
of the created virtual machine hosting the database (`test/test_local.py` and `test/test_s3.py`).

In case of test failures the currently provisioned test machine can be reached via ssh with

```bash
./do test-ssh
```
