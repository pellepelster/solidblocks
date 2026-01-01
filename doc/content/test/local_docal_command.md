+++
title = 'Command test context'
description = 'Run and assert commands locally and inside of Docker images'
+++

The local and docker command contexts allow to run commands and scrips on the machine executing the Junit tests and verify the outcomes.

#### Run command or script
```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/command/LocalCommandContext.kt" %}}
```

#### Run command or script inside Docker
```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/command/DockerCommandContext.kt" %}}
```

#### Options

Both the local and the docker command execution can be configured before the command is executed

```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/command/LocalCommandOptions.kt" %}}
```

#### Assertions

The following assertions are available for the results of `local().command(...).runResult()` as well as `docker(DockerTestImage.UBUNTU_22).command(...).runResult()`

```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/command/CommandAssertions.kt" %}}
```
