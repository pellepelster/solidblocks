+++
title = 'Kotlin API'
weight = 70
description = 'Kotlin API for the Hetzner Cloud'
overviewGroup = "util"
faIcon = "fa-brands fa-java"
+++


`solidblocks-hetzner-cloud` is Kotlin library for accessing the Hetzner Cloud API.

## Usage

To use `solidblocks-hetzner-cloud` just add the dependency to your Gradle or Maven build

### build.gradle.kts
```groovy
{{% include "/snippets/solidblocks-test-gradle/build.gradle.kts" %}}
```


{{% notice info %}}
The implementation is currently driven by the needs of other projects I am working and, and does not yet fully cover all API endpoints. See development chapter on how to add missing features.
{{% /notice %}}
