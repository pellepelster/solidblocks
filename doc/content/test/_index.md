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

### extension usage
```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/ExtensionUsage.kt" %}}
```

or use the factory methods to create the test context directly

### factory method usage
```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/FactoryMethodUsage.kt" %}}
```

> [!INFO]
> When used from the extension, all created resources are automatically managed and cleaned up when the tests are finished.

## Logging

All produced log output is marked with its origin, to make it easy to distinguish it from other logs during a test run. The following snippets contains an example log with some log statements form the library itself `[INFO]`, logs from forked commands `[STDOUT]` and logs from a cloud-init run `[CLOUDINIT]` of a provisioned server.

```shell
[     INFO] Terraform checksums already downloaded at '/home/pelle/git/solidblocks/solidblocks-test/.cache/terraform_1.14.2_SHA256SUMS'
[     INFO] Terraform already downloaded at '/home/pelle/git/solidblocks/solidblocks-test/.cache/terraform_1.14.2_linux_amd64.zip'
[     INFO] Extracting Terraform binary to '/home/pelle/git/solidblocks/solidblocks-test/.bin/1.14.2/terraform'
[     INFO] Terraform installed at '/home/pelle/git/solidblocks/solidblocks-test/.bin/1.14.2/terraform'
[     INFO] running '/home/pelle/git/solidblocks/solidblocks-test/.bin/1.14.2/terraform init -upgrade' in '/home/pelle/git/solidblocks/solidblocks-test/build/resources/test/terraformCloudInitTestBed1'
[   STDOUT] Initializing the backend...
[   STDOUT] Initializing provider plugins...
[   STDOUT] - Finding hetznercloud/hcloud versions matching ">= 1.48.0"...
[   STDOUT] - Finding hashicorp/random versions matching "3.7.2"...
[   STDOUT] - Finding hashicorp/tls versions matching "4.1.0"...
[   STDOUT] - Using previously-installed hashicorp/tls v4.1.0
[   STDOUT] - Using previously-installed hetznercloud/hcloud v1.57.0
[   STDOUT] - Using previously-installed hashicorp/random v3.7.2

...

[   STDOUT] private_key_openssh_ecdsa = <sensitive>
[   STDOUT] private_key_openssh_ed25519 = <sensitive>
[   STDOUT] private_key_openssh_rsa = <sensitive>
[   STDOUT] private_key_pem_ecdsa = <sensitive>
[   STDOUT] private_key_pem_ed25519 = <sensitive>
[   STDOUT] private_key_pem_rsa = <sensitive>
[CLOUDINIT] Cloud-init v. 22.4.2 running 'init-local' at Wed, 24 Dec 2025 13:27:44 +0000. Up 8.65 seconds.
[CLOUDINIT] Cloud-init v. 22.4.2 running 'init' at Wed, 24 Dec 2025 13:27:48 +0000. Up 11.98 seconds.

...

[CLOUDINIT] ci-info: +-------+-------------------------+---------+-----------+-------+
[CLOUDINIT] ci-info: | Route |       Destination       | Gateway | Interface | Flags |
[CLOUDINIT] ci-info: +-------+-------------------------+---------+-----------+-------+
[CLOUDINIT] ci-info: |   0   | 2a01:4f9:c010:957b::/64 |    ::   |    eth0   |   U   |
[CLOUDINIT] ci-info: |   1   |        fe80::/64        |    ::   |    eth0   |   U   |
[CLOUDINIT] ci-info: |   2   |           ::/0          | fe80::1 |    eth0   |   UG  |
[CLOUDINIT] ci-info: |   4   |          local          |    ::   |    eth0   |   U   |
[CLOUDINIT] ci-info: |   5   |          local          |    ::   |    eth0   |   U   |
[CLOUDINIT] ci-info: |   6   |        multicast        |    ::   |    eth0   |   U   |
[CLOUDINIT] ci-info: +-------+-------------------------+---------+-----------+-------+

```

## Test Contexts

Different test functions and assertions are grouped logically in test contexts, and can assert different infrastructure related properties and behaviors.

### Local and docker command context

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

### Terraform

The terraform context allows to apply Terraform configurations from within the test code. It exposes the full `init()`, `apply()` and `destroy()` lifecycle that can be used either to test Terraform modules, or to prepare an environment for other tests. 
When used the test context as well as the factory methods will always download the correct version for the architecture executing the tests, supporting Linux, MacOS and Windows on `x86` and `arm`.


#### Test a Terraform config

Given a folder containing Terraform files, like for example

##### /module1/main.tf
```terraform
{{% include "/snippets/solidblocks-test-gradle/src/test/resources/module1/main.tf" %}}
```

The resources described in this file can be created using the Terraform test context

##### Test a terraform setup
```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/terraform/TerraformContext.kt" %}}
```

##### Options

```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/terraform/TerraformOptions.kt" %}}
```

### Host

The host context allows to verify properties of remote hosts.

```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/host/HostContext.kt" %}}
```

#### Assertions

```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/host/HostAssertions.kt" %}}
```

### SSH

The SSH context allows assertions to run on remote hosts. Prerequisites are a running SSH server on the host provided by `<ssh_host>` along with a matching `<private_key>`. The private key can be an `pem` or `openssh` encoded `rsa`, `ed25519` or `ecdsa` key.

```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/ssh/SSHContext.kt" %}}
```

#### Assertions

`SSHContext.command(...)` supports the same assertions as `local().command(...).runResult()` and `docker(...).command(...).runResult()`.


### Cloud-Init

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
