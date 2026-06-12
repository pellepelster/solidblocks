+++
title = 'backup_local'
description = 'Enables use of locally attached disks for service backups'
weight = 50
+++

The `backup_local` provider enables use of locally attached disks for service backups. For each service a dedicated backup volume is created and attached to the service's virtual machine, the volume size can be controlled per service via the `backup_size` option.

## Example

```yaml
name: cloud1

providers:
  - type: pass
  - type: ssh_key
  - type: hcloud
  - type: backup_local

services:
  #...
```

See the [configuration format]({{% relref "../configuration/format.md" %}}) for the full keyword reference.
