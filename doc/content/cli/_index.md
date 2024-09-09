---
title: CLI
weight: 90
description: Various tooling for infrastructure provisioning
---

The Soliblocks CLI (`blcks`) is available for all major platforms and provides tools around infrastructure provisioning

# Installation

Latest release can be downloaded from the [Soldiblocks releases page](https://github.com/pellepelster/solidblocks/releases/latest) 

## Linux

```shell
curl -L https://github.com/pellepelster/solidblocks/releases/download/{{% env "SOLIDBLOCKS_VERSION" %}}/blcks-linuxX64-{{% env "SOLIDBLOCKS_VERSION" %}} -o blcks
chmod +x blcks
./blcks --help
```

## Windows

```shell
curl -L https://github.com/pellepelster/solidblocks/releases/download/{{% env "SOLIDBLOCKS_VERSION" %}}/blcks-mingwX64-{{% env "SOLIDBLOCKS_VERSION" %}} -o blcks
chmod +x blcks
./blcks --help
```

## macOS (Intel)

```shell
curl -L https://github.com/pellepelster/solidblocks/releases/download/{{% env "SOLIDBLOCKS_VERSION" %}}/blcks-macosX64-{{% env "SOLIDBLOCKS_VERSION" %}} -o blcks
chmod +x blcks
./blcks --help
```

## macOS (Arm)

```shell
curl -L https://github.com/pellepelster/solidblocks/releases/download/{{% env "SOLIDBLOCKS_VERSION" %}}/blcks-macosArm64-{{% env "SOLIDBLOCKS_VERSION" %}} -o blcks
chmod +x blcks
./blcks --help
```

# Commands

{{% children description="true" %}}
