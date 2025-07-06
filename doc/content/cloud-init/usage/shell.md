---
title: Shell
weight: 10
description: Use cloud-init function in plain shell scripts    
---

To use Solidblocks cloud-init in your cloud VM, use this snippet to bootstrap it in a [cloud init user data script](https://cloudinit.readthedocs.io/en/latest/explanation/format.html). After `solidblocks_bootstrap_cloud_init` is executed, all cloud-init functions can be used.

```shell
{{% includef "/snippets/solidblocks-cloud-init-bootstrap-%s.sh" %}}
```