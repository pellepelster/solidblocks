---
title: Software
weight: 40
disableToc: true
---

Based on the functionality from [Download]({{%relref "download/_index.md" %}}) and [File]({{%relref "download/_index.md" %}}) this library
offers automatic installation for various software packages.
The packages will be downloaded to a local `.cache` directory, extracted to `.bin` and executed from there. This helps to avoid polluting the system with software only needed for your project and also avoids version conflicts in case other projects use different versions of the same tool.
When all needed software is downloaded you can extend `$PATH` to also include the just downloaded software, either manually

```shell
software_ensure_shellcheck
export PATH="${PATH}:$(software_export_path)"

shellcheck --version
```

or using the wrapper function 


```shell
software_ensure_hugo
software_ensure_export_path

hugo version
```

## Functions

## `software_ensure_hugo(version, checksum)`


