---
title: Network
description: Networking Utilities
---

## Functions

### `network_wait_for_port_open(host, port, period=1)` {#network_wait_for_port_open}

Wait indefinitely for `${port}` to be open on `${host}`, poll every `${period}` seconds.

```shell
source "network.sh"

network_wait_for_port_open "pelle.io" 22
```

