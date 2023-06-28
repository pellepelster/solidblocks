---
title: Cloud-Init
weight: 20
description: Based on Solidblocks Shell reusable shell functions for typical Cloud-Init usage scenarios
---

Based on [Shell]({{%relref "shell/_index.md" %}}) reusable shell functions for typical [Cloud-Init](https://cloudinit.readthedocs.io/en/latest/) user-data usage scenarios.

{{% children description="true" %}}

## Architecture

To work around the size limitation of user-data scripts and also make the scripts easier to maintain and test, Solidblocks cloud init is distributed as a compressed archive, that automatically gets downloaded and extracted to `/solidblocks/...` on the cloud VM.
After running the `bootstrap_solidblocks` function all library functions can be used, like shown in the example below.

## Testing

To ensure functionality of all library functions a full integration test for `x86` and the `arm` platform is executed from `test` using 
[testinfra](https://testinfra.readthedocs.io/en/latest/) asserting the results in `test/test_cloud_init.py` 


## Kitchen Sink Usage Example

```shell
{{% include "/snippets/cloud_init_kitchen_sink.sh" %}}
```
