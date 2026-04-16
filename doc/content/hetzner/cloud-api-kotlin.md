+++
title = 'Cloud API Kotlin'
weight = 70
description = 'Kotlin API for the Hetzner Cloud'
overviewGroup = "lib"
faIcon = "fa-code"
+++

Solidblocks `hetzner-cloud` is Kotlin implementation for the Hetzner Cloud API.

## Usage

To use Solidblocks `hetzner-cloud` just add the dependency to your Gradle or Maven build

### build.gradle.kts
```kotlin
{{% include-safe "/snippets/solidblocks-hetzner-cloud-example/build.gradle.kts" %}}
```

### Basics

The library supports Kotlin coroutines, thus all API calls must be made from a coroutine context. The root api has to be initialized using a Hetzner cloud api token, from there all supports resources are available grouped by resource type.

```kotlin
{{% include-safe "/snippets/solidblocks-hetzner-cloud-example/src/main/kotlin/de/solidblocks/hetzner/BasicUsage.kt" %}}
```

### Create and Get

All resources support listing, as well as filtering by resource attributes or labels

```kotlin
{{% include-safe "/snippets/solidblocks-hetzner-cloud-example/src/main/kotlin/de/solidblocks/hetzner/CreateAndGetResources.kt" %}}
```

### Listing and Filtering

All resources support resource creation and retrieval by name and id.

```kotlin
{{% include-safe "/snippets/solidblocks-hetzner-cloud-example/src/main/kotlin/de/solidblocks/hetzner/Filtering.kt" %}}
```

### Actions

Waiters are available for resources that are created or updated asynchronosly

```kotlin
{{% include-safe "/snippets/solidblocks-hetzner-cloud-example/src/main/kotlin/de/solidblocks/hetzner/WaitForActions.kt" %}}
```

### Exceptions

For failing API calls the exception `HetznerApiException` provides type access to the underlying Hetzner error.

```kotlin
{{% include-safe "/snippets/solidblocks-hetzner-cloud-example/src/main/kotlin/de/solidblocks/hetzner/Exceptions.kt" %}}
```
