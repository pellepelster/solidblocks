+++
title = 'CLI'
description = 'Command line tooling for infrastructure provisioning and operation'
overviewGroup = "util"
faIcon = "fa-terminal"
+++

The Soliblocks CLI (`blcks`) is available for all major platforms and provides tools around infrastructure provisioning

# Installation

Latest release can be downloaded from the [Soldiblocks releases page](https://github.com/pellepelster/solidblocks/releases/latest) 

## Linux

```shell
curl -L https://github.com/pellepelster/solidblocks/releases/download/v{{% env "SOLIDBLOCKS_VERSION" %}}/solidblocks-cli-linux-amd64-v{{% env "SOLIDBLOCKS_VERSION" %}}.zip -o solidblocks-cli-linux-amd64-v{{% env "SOLIDBLOCKS_VERSION" %}}.zip
unzip solidblocks-cli-linux-amd64-v{{% env "SOLIDBLOCKS_VERSION" %}}.zip
chmod +x blcks
./blcks --help
```

## Windows

```shell
curl -L https://github.com/pellepelster/solidblocks/releases/download/v{{% env "SOLIDBLOCKS_VERSION" %}}/solidblocks-cli-windows-amd64-v{{% env "SOLIDBLOCKS_VERSION" %}}.zip -o solidblocks-cli-windows-amd64-v{{% env "SOLIDBLOCKS_VERSION" %}}.zip
unzip solidblocks-cli-windows-amd64-v{{% env "SOLIDBLOCKS_VERSION" %}}.zip
chmod +x blcks
./blcks --help
```

## macOS (Intel)

```shell
curl -L https://github.com/pellepelster/solidblocks/releases/download/v{{% env "SOLIDBLOCKS_VERSION" %}}/solidblocks-cli-darwin-amd64-v{{% env "SOLIDBLOCKS_VERSION" %}}.zip -o solidblocks-cli-darwin-amd64-v{{% env "SOLIDBLOCKS_VERSION" %}}.zip
unzip solidblocks-cli-darwin-amd64-v{{% env "SOLIDBLOCKS_VERSION" %}}.zip
chmod +x blcks
./blcks --help
```

## macOS (Arm)

```shell
curl -L https://github.com/pellepelster/solidblocks/releases/download/v{{% env "SOLIDBLOCKS_VERSION" %}}/solidblocks-cli-darwin-arm64-v{{% env "SOLIDBLOCKS_VERSION" %}}.zip -o solidblocks-cli-darwin-arm64-v{{% env "SOLIDBLOCKS_VERSION" %}}.zip
unzip solidblocks-cli-darwin-arm64-v{{% env "SOLIDBLOCKS_VERSION" %}}.zip
chmod +x blcks
./blcks --help
```

# Commands

{{% children description="true" %}}
