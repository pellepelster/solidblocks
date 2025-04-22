+++
title = "Cloud-Init"
weight = 20
description = "Based on Solidblocks Shell reusable functions for Cloud-Init usage"
overviewGroup = "shell"
faIcon = "fa-terminal"
+++

Based on [Shell]({{%relref "shell/_index.md" %}}) reusable shell functions for typical [Cloud-Init](https://cloudinit.readthedocs.io/en/latest/) user-data usage scenarios.

{{% children description="true" %}}

## Architecture

To work around the size limitation of user-data scripts and also make the scripts easier to maintain and test, Solidblocks cloud-init is distributed as a compressed archive, that automatically gets downloaded and extracted to `/solidblocks/...` on the cloud VM. After running the `solidblocks_cloud_init_bootstrap` function all library functions can be used, like shown in the example below.

## Testing

To ensure functionality of all cloud-init library functions, a full integration test for all use-cases can be executed with `./do test` where cloud resources are created from `test/${test_case}` and functionality is asserted using [testinfra](https://testinfra.readthedocs.io/en/latest/) in `test/test_${test_case}.py` 
