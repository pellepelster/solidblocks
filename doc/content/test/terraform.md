+++
title = 'Terraform test context'
description = 'Run and assert Terraform modules'
+++

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

The following options can be set to configure the Terraform process

```kotlin
{{% include "/snippets/solidblocks-test-gradle/src/test/kotlin/solidblocks/test/gradle/terraform/TerraformOptions.kt" %}}
```
