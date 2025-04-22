+++
title = "Development"
description = "Guide for development of Solidblocks"
overviewGroup = "dev"
+++

## Repository Structure

Solidblocks uses a mono-repository approach where each component resides in a separate folder. Each component has a `do` file that allows each component to be built and tested locally as well as in the CI. The `do` file in the repository root orchestrates the overall build process and includes component-agnostic tasks like generation of the documentation
etc.

```shell
/do build
/solidblocks-shell/do test
/solidblocks-rds-postgresql/do clean
[...]
```

Common for all `do` files are the following tasks:

* `./do build` Build the component
* `./do test` Run all tests for the component
* `./do clean` Clean up ephemeral resources like local files and cloud resources
* `./do format` Apply formatters and linters to all sourcecode of the component
* `./do release-prepare` prepare a release, commonly used to insert correct version numbers into documentation and code snippets  
* `./do release-test` run tests against released artifacts, commonly used to verify that the code snippets work  
* `./do release-artifacts` releases additional artifacts, e.g. a tested docker image without the `-rc` version postfix

## Documentation

The documentation is based on [hugo](https://gohugo.io/). Each component contributing source code snippets to the documentation should do so by adding the snippets to the components `build/snippets` folder, so after running `./do build-documentation` they can be included like this:

```shell
{{%/* include "/snippets/shell-bootstrap-solidblocks.sh" */%}}
```

Snippets should always be tested if feasible to ensure the documentation is correct and runnable.

## Versioning

Versions are derived from the current git context, and default to snapshot if no information is available or injected from CI environment variables. 

```shell
VERSION="${GITHUB_REF_NAME:-snapshot}"
```

### Docker Artifacts

To pass docker artifacts between build steps without accidentally releasing an untested docker image, freshly built and not yet tested images are tagged with a `-rc` postfix in the tag, e.g. `ghcr.io/pellepelster/solidblocks-rds-postgresql:${VERSION}-rc` and re-tagged during the release process after all tests are run to `ghcr.io/pellepelster/solidblocks-rds-postgresql:${VERSION}`

## Tests

Especially the infrastructure heavy components of Solidblocks rely on downloading released code from Github releases. To be able to mimic this behaviour during integration tests, all code using released code from Github should provide the ability to override the release server to allow for injecting of development code during integration tests:

```shell
curl -v -L "${SOLIDBLOCKS_BASE_URL:-https://github.com}/pellepelster/[...]"
```

For code where it is not feasible to inject a local webserver (e.g. code running on a cloud provider in cloud-init) AWS S3 is used as a webserver because it is easily scriptable.
Tests that make uses of cloud VM instances are expected to create a temporary `ssh_config` for a host named `test` that can be used to log into the created machine of the currently running test via `ssh -F <path>/ssh_config test`, see for example `testbeds/hetzner/ssh-config/ssh_config.template`


## Secrets

For cloud provider specific integration tests credentials are needed, that are either taken from environment variables, or if not set pulled from a
local [pass](https://www.passwordstore.org/)-based password store.

### Cloud Accounts

Components that work on cloud resources come with full integration tests using a real cloud backend.
It is highly advised to create separate cloud accounts for test executions. 

#### AWS

Access to a dedicated AWS account  via `AWS_ACCESS_KEY_ID` (`pass solidblocks/aws/test/access_key_id`) and `AWS_SECRET_ACCESS_KEY` (`pass solidblocks/aws/test/secret_access_key`) is needed with the following permissions:  

#### S3
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "VisualEditor0",
            "Effect": "Allow",
            "Action": "s3:*",
            "Resource": "arn:aws:s3:::test-*/**"
        },
        {
            "Sid": "VisualEditor1",
            "Effect": "Allow",
            "Action": "s3:ListAllMyBuckets*",
            "Resource": "*"
        },
        {
            "Sid": "VisualEditor2",
            "Effect": "Allow",
            "Action": "s3:*",
            "Resource": "arn:aws:s3:::test-*"
        }
  ]
}
```

##### DynamoDB
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "VisualEditor0",
            "Effect": "Allow",
            "Action": "dynamodb:*",
            "Resource": "arn:aws:dynamodb:*:*:table/test-*"
        },
        {
            "Sid": "VisualEditor1",
            "Effect": "Allow",
            "Action": "dynamodb:List*",
            "Resource": "arn:aws:dynamodb:*:*:table/*"
        },
        {
            "Sid": "VisualEditor2",
            "Effect": "Allow",
            "Action": [
                "dynamodb:List*",
                "dynamodb:Describe*"
            ],
            "Resource": "arn:aws:dynamodb:*:*:table/*"
        }
  ]
}
```

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