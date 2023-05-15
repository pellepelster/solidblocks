---
title: Storage
weight: 10
description: Utilities for storage handling
---

Utilities for storage handling

## Functions

### `storage_mount(storage_device, storage_dir)` {#storage_mount}

Waits for `storage_device` to appear and be ready, and then mounts it to `storage_dir` and ensure it is mounted on reboot by adding it to `/etc/fstab`.

```shell
storage_mount "/dev/sdb1" "/data1"
```

