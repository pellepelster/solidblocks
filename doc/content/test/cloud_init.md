+++
title = 'Cloud-Init test context'
description = 'Asserts cloud-init runs via SSH'
+++

The cloud-init context allows assertions based on the artifacts generated after a cloud-init run. Like in the SSH context, the connection to the machine is created via SSH, so the same prerequisites as for the SSH context apply.

```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/cloudinit/CloudInitContext.kt" %}}
```

> [!NOTE]
> By enabling `CloudInitTestContext.printOutputLogOnTestFailure` the output from `/var/log/cloud-init-output.log`
> will be printed if a test fails, please be aware that this might leak credentials. that are processed in a cloud-init script.

#### Assertions

```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/cloudinit/CloudInitAssertions.kt" %}}
```
