+++
title = "Development"
description = "Guide for development of Solidblocks"
overviewGroup = "dev"
weight = 200
+++

## Repository Structure

Solidblocks uses a mono-repository approach where each component resides in a separate folder. The build orchestration and environment setup is done by [mise](https://mise.jdx.dev/), where each component has a `mise.toml` file that allows each component to be built and tested locally as well as in the CI. The root `mise.toml` file in the repository root orchestrates the overall build process and includes component-agnostic tasks like generation of the documentation
etc.

```shell
mise //...:build
mise //solidblocks-shell:test
[...]
```

Common for all `mise.toml` files are the following tasks:

* `mise :build` Build the component
* `mise :test` Run all tests for the component
* `mise :clean` Clean up ephemeral resources like local files and cloud resources
* `mise :format` Apply formatters and linters to all sourcecode of the component
* `mise :release-prepare` prepare a release, commonly used to insert correct version numbers into documentation and code snippets  
* `mise :release-test` run tests against released artifacts, commonly used to verify that the code snippets work  
* `mise :release-artifacts` releases additional artifacts, e.g. a tested docker image without the `-rc` version postfix

## Conventions

The follwing environment variables can be used to influence the build if supported by the component.

`BUILD_FAST` If set to `true` build are shortened, e.g. by leaving out variants or skipping optimizations. Release build should never uses this.

`SKIP_TESTS` If set to `integration` resource heavy or time intensive integration tests are skipped, ideal for quick verification builds.

`DOCKER_PLATFORM´ Target platform to build docker images for, defaults to `linux/amd64,linux/arm64`.

To run a quick local build

```shell
export BUILD_FAST="true"
export SKIP_TESTS="integration"
export DOCKER_PLATFORM="linux/amd64"
mise //...:build
mise //...:test
```

## Documentation

The documentation is based on [hugo](https://gohugo.io/). Each component contributing source code snippets to the documentation should do so by adding the snippets to the components `build/snippets` folder, so after running `mise :documentation-build` they can be included like this:

```shell
{{%/* include "/snippets/shell-bootstrap-solidblocks.sh" */%}}
```

Snippets should always be tested if feasible to ensure the documentation is correct and runnable.

## Versioning

Solidblocks uses semver compliant versioning, where the release version is derived from the git tag. The tag version is prefix with a `v` eg. `v1.2.3`. The version is stored in the environment variable `VERSION` and is automatically populated from the git context and defaults to `0.0.0` on non-tag references. In the environment variable `VERSION` and through the whole build system the variable is stored without the `v` prefix. The prefix is only added before the artifacts are written to the `build` directory (except for Python artifacts since they have to comply with the PEP 440 versioning standard).


### Docker Artifacts

To pass docker artifacts between build steps without accidentally releasing an untested docker image, freshly built and not yet tested images are tagged with a `-snapshot` postfix in the tag, e.g. `ghcr.io/pellepelster/solidblocks-rds-postgresql:v${VERSION}-snapshot` and re-tagged during the release process after all tests are run to `ghcr.io/pellepelster/solidblocks-rds-postgresql:v${VERSION}`

## Tests

Especially the infrastructure heavy components of Solidblocks rely on downloading released code from Github releases. To be able to mimic this behavior during integration tests, all code using released code from Github should provide the ability to override the release server to allow for injecting of development code during integration tests:

```shell
curl -L "${SOLIDBLOCKS_BASE_URL:-https://github.com}/pellepelster/[...]"
```

For code where it is not feasible to inject a local webserver (e.g. code running on a cloud provider in cloud-init) AWS S3 is used as a webserver because it is easily scriptable. Tests that make uses of cloud VM instances are expected to create a temporary `ssh_config` for a host named `test` that can be used to log into the created machine of the currently running test via `ssh -F <path>/ssh_config test`, see for example `testbeds/hetzner/ssh-config/ssh_config.template`

## Secrets

For cloud provider specific integration tests credentials are needed, that are either taken from environment variables, or if not set pulled from a local [pass](https://www.passwordstore.org/)-based password store.

### Cloud Accounts

Components that work on cloud resources come with full integration tests using a real cloud backend. It is highly advised to create separate cloud accounts for test executions. 

#### AWS

Access to a dedicated AWS account  via `AWS_ACCESS_KEY_ID` (`pass solidblocks/aws/test/access_key_id`) and `AWS_SECRET_ACCESS_KEY` (`pass solidblocks/aws/test/secret_access_key`) is needed with the following permissions:  

An appropiate `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` with minimal permissions can be created by calling

````
./do test-init-aws
````

{{% notice warning %}}
All resources included in the account will be cleaned to ensure a consistent test environment
{{% /notice %}}

##### Hetzner

Read-Write access to a dedicated Hetzner cloud project via `HCLOUD_TOKEN` (`pass solidblocks/hetzner/hcloud_api_token`).

{{% notice warning %}}
All resources included in the project will be cleaned to ensure a consistent test environment
{{% /notice %}}

##### Google Cloud

A dedicated testing service role with minimal access with a service account key available under `GCP_SERVICE_ACCOUNT_KEY` ( or in pass at `solidblocks/gcp/test/service_account_key`).

{{% notice warning %}}
All resources included in the project will be cleaned to ensure a consistent test environment
{{% /notice %}}