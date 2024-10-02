---
title: DNS
weight: 70
description: Assertions and JUnit extensions for infrastructure testing 
---

Kotlin API for the [Hetzner DNS Api](https://dns.hetzner.com/api-docs). Documentation is pending since the API is still developing, for an introduction please see this post [Hetzner DNS Api](https://pelle.io/posts/hetzner-dns-api/).

# Usage

To use `solidblocks-dns` just add the dependency to your Gradle build

```groovy
// [...]

dependencies {
// [...]
    implementation("de.solidblocks:infra-test:{{% env "SOLIDBLOCKS_VERSION" %}}")
}
```

`solidblocks-dns` uses `kotlin.Result` to provide success/failure information for the underlying HTTP calls against the
Hetzner DNS API.

```kotlin
val api = HetznerDnsApi(System.getenv("HETZNER_DNS_API_TOKEN"))

val createdZone = api.createZone(ZoneRequest("my-new-zone.de")).getOrThrow()
println("created zone with id ${createdZone.zone.id}")

val createdRecord =
    api.createRecord(RecordRequest(createdZone.zone.id, RecordType.A, "www", "192.168.0.1")).getOrThrow()
println("created record with id ${createdRecord.record.id}")
```