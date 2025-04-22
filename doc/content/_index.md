+++
title = "Solidblocks"
+++

Solidblocks is a library of reusable components for infrastructure operation, automation and developer experience. It consists of several components, each covering a different infrastructure automation aspects. 

## Design Goals

* **Easy Reuse** Each component should easily be embeddable into any setup without requiring any specific project setup or structure
* **Composable** Components be combinable with each other using defined interfaces reducing the need for configuration
* **Tested** Components should be tested, in particular covering potential failure cases
* **Resilience** Components should be immune against temporary outages, especially in regard to networking and storage
* **Logging** Components should give helpful feedback for during normal operation and especially when errors occur


## Components

{{<component-overview>}}

## Development

See [Development]({{%relref "development/_index.md" %}}) for hints on developing Solidblocks. 