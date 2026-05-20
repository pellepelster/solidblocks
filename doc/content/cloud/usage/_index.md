+++
title = 'Usage'
weight = 10
+++

Solidblocks cloud follows a plan and apply pattern similar to Terraform.

## Planning

To get an overview of all resources that will be changed/created/deleted run the plan command

```shell
blcks cloud plan <cloud_config>
```

## Apply

To roll out those changes use the apply command

```shell
blcks cloud apply <cloud_config>
```

### Rotate all secrets

using the `--taint-secrets` flag forces re-creation of all auto-generated secrets, helpful in situations where ad-hoc credentials rotation is needed

```shell
blcks cloud apply --taint-secrets <cloud_config>
```

## IDE support

If your IDE supports YAML language server annotations you can enable context-aware autocompletion by adding the following annotation to your Solidbocks Cloud configuration

```shell
# yaml-language-server: $schema=https://solidblocks.de/blcks-cloud.schema.json
```

Visual Studio Code does not support this out of the box, but I can be retrofitted using the Redhat YAML lnguage server plugin

```shell
code --install-extension redhat.vscode-yaml
``` 

The following video show the syntax completion in action

<video controls width="100%" style="background: transparent; display: block;">
  <source src="yaml_lang_server.mp4" type="video/mp4">
</video>

