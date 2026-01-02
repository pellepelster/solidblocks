+++
title = 'Test'
weight = 70
description = 'Assertions and JUnit extensions for infrastructure testing'
overviewGroup = "util"
faIcon = "fa-brands fa-java"
+++


Solidblocks `infra-test` is an infrastructure testing library for writing unit tests in Java/Kotlin to test the state of your servers and other resources.

## Usage

To use `solidblocks-test` just add the dependency to your Gradle or Maven build

### build.gradle.kts
```groovy
{{% include "/snippets/solidblocks-test-gradle/build.gradle.kts" %}}
```

Depending on your use case, you can either inject the test context using the `SolidblocksTest` test extension

### Extension usage
```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/ExtensionUsage.kt" %}}
```

or use the factory methods to create the test context directly

### Factory method usage
```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/FactoryMethodUsage.kt" %}}
```

> [!INFO]
> When used from the extension, all created resources are automatically managed and cleaned up when the tests are finished.

## Logging

All produced log output is marked with its origin, to make it easy to distinguish it from other logs during a test run. The following snippets contains an example log with some log statements form the library itself `[INFO]`, logs from forked commands `[STDOUT]` and logs from a cloud-init run `[CLOUDINIT]` of a provisioned server.

```shell
[   INFO] creating test context with id '4B8YUHUMAKZJ' for '[engine:junit-jupiter]/[class:de.solidblocks.infra.test.CloudInitTest]/[method:testCloudInitSuccess(de.solidblocks.infra.test.SolidblocksTestContext)]'
[   INFO] Terraform checksums already downloaded at '/home/pelle/git/solidblocks/solidblocks-test/.cache/terraform_1.14.2_SHA256SUMS'
[   INFO] Terraform already downloaded at '/home/pelle/git/solidblocks/solidblocks-test/.cache/terraform_1.14.2_linux_amd64.zip'
[   INFO] Extracting Terraform binary to '/home/pelle/git/solidblocks/solidblocks-test/.bin/1.14.2/terraform'
[   INFO] Terraform installed at '/home/pelle/git/solidblocks/solidblocks-test/.bin/1.14.2/terraform'

...

[   INFO] [STDOUT] Initializing the backend...
[   INFO] [STDOUT] Initializing provider plugins...
[   INFO] [STDOUT] - Finding hashicorp/tls versions matching "4.1.0"...
[   INFO] [STDOUT] - Finding hetznercloud/hcloud versions matching ">= 1.48.0"...
[   INFO] [STDOUT] - Finding hashicorp/random versions matching "3.7.2"...
[   INFO] [STDOUT] - Using previously-installed hashicorp/tls v4.1.0
[   INFO] [STDOUT] - Using previously-installed hetznercloud/hcloud v1.57.0
[   INFO] [STDOUT] - Using previously-installed hashicorp/random v3.7.2

...

[   INFO] [CLOUDINIT] Cloud-init v. 22.4.2 running 'init-local' at Thu, 01 Jan 2026 20:56:42 +0000. Up 7.31 seconds.
[   INFO] [CLOUDINIT] Cloud-init v. 22.4.2 running 'init' at Thu, 01 Jan 2026 20:56:46 +0000. Up 10.37 seconds.

...

[   INFO] [CLOUDINIT] Generating public/private rsa key pair.
[   INFO] [CLOUDINIT] Your identification has been saved in /etc/ssh/ssh_host_rsa_key
[   INFO] [CLOUDINIT] Your public key has been saved in /etc/ssh/ssh_host_rsa_key.pub
[   INFO] [CLOUDINIT] The key fingerprint is:
[   INFO] [CLOUDINIT] SHA256:DyJc2R3djWtYHeEo4gLD8B5dHJNS7XUgtrZT6WZ9R1Y root@5dv5n16f
[   INFO] [CLOUDINIT] The key's randomart image is:
[   INFO] [CLOUDINIT] +---[RSA 3072]----+
[   INFO] [CLOUDINIT] |   .    o==+ o.*E|
[   INFO] [CLOUDINIT] |    + .+.+oo+.B.+|
[   INFO] [CLOUDINIT] |     *o.o.o+.*.oo|
[   INFO] [CLOUDINIT] |   ...+ . o.* +o |
[   INFO] [CLOUDINIT] |    o...S. o = .o|
[   INFO] [CLOUDINIT] |     . ..o  +   o|

...
```

## Test Contexts

Different test functions and assertions are grouped logically in test contexts, and can assert different infrastructure related properties and behaviors. Tests contexts can also be derived from other test contexts, for example a `CloudInitTestContext` ❶ can directly be derived from the `SSHTestcontext` because it also uses SSH under the hood.

Resources created from the different test contexts are cleaned up when all tests are run, except when the `cleanupAfterTestFailure` is set to `false` which then leaves everything in place for debugging purposes.  

```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/TestContexts.kt" %}}
```
