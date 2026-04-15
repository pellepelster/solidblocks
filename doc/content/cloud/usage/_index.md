+++
title = 'Usage'
+++

# IDE support

When your IDE supports YAML language server annotations you can enable context-aware autocompletion by adding the following annotation to your Solidbocks Cloud configuration

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