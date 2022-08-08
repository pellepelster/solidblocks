---
title: Software
weight: 40
disableToc: true
---

Based on the functionality from [Download]({{%relref "download/_index.md" %}}) and [File]({{%relref "download/_index.md" %}}) this library offers automatic installation for various software packages.

The packages will be downloaded to a local `.cache` directory, extracted to `.bin` and executed from there. This helps to avoid polluting the system with software only needed for your project and also avoids version conflicts in case other projects use different versions of the same tool.

{{% notice info %}}
The `.cache` and `.bin` directory can be overwritten by setting `$CACHE_DIR` and `$BIN_DIR` before sourcing in the libraries. This can be useful in a continuous integration env to put the cache on a persistent storage to avoid re-downloading the software on every build. 
{{% /notice %}}

When all needed software is downloaded it can be automatically added to the search `$PATH` via `software_set_export_path`

```shell
software_ensure_hugo

software_set_export_path

hugo version
```

or manually byt setting the `$PATH` directly 

```shell
software_ensure_shellcheck

export PATH="${PATH}:$(software_export_path)"

shellcheck --version
```

## Functions

All functions can be called without any parameters and will then install the software in a recent version. An exact version and checksum for the downloaded can also be given to force a specific version.

```shell
software_ensure_terraform

software_ensure_terraform "0.13.4" "a92df4a151d390144040de5d18351301e597d3fae3679a814ea57554f6aa9b24"
```


## `software_ensure_terraform(version, checksum)`
## `software_ensure_consul(version, checksum)`
## `software_ensure_hugo(version, checksum)`
## `software_ensure_shellcheck(version, checksum)`
