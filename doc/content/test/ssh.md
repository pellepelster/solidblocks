+++
title = 'SSH test context'
description = 'Run tests on remote hosts via SSH'
+++

The SSH context allows assertions to run on remote hosts. Prerequisites are a running SSH server on the host provided by `<ssh_host>` along with a matching `<private_key>`. The private key can be an `pem` or `openssh` encoded `rsa`, `ed25519` or `ecdsa` key.

```kotlin
{{% include-safe "snippets/solidblocks-test-example/src/test/kotlin/de/solidblocks/test/ssh/SSH.kt" %}}
```

#### Assertions

`SSHContext.command(...)` supports the same assertions as `local().command(...).runResult()` and `docker(...).command(...).runResult()`.

```kotlin
{{% include-safe "snippets/solidblocks-test-example/src/test/kotlin/de/solidblocks/test/ssh/SSHAssertions.kt" %}}
```
