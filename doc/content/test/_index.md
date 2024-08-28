---
title: Test
weight: 70
description: Assertions and JUnit extensions for infrastructure testing 
---

Documentation is pending since the API is still developing, for an introduction please see this post [Infrastructure testing with Solidblocks](https://pelle.io/posts/solidblocks-test/)

# Usage

To use solidblocks-test just add the dependency into your Gradle build

```groovy
{{% include "/snippets/solidblocks-test-gradle/build.gradle.kts" %}}
```

And extend your test classes with `SolidblocksTest` to get the `SolidblocksTestContext`

```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/CommandTest.kt" %}}
```