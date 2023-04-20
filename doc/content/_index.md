---
title: "Solidblocks"
---

# Solidblocks

Solidblocks is a library of reusable components for infrastructure operation, automation and developer experience. It consists of several components, each covering a different infrastructure aspect. 

## Design Goals

* **Easy Reuse** Each component should easily be embeddable into any setup without requiring any specific project setup or structure
* **Tested** Components should be tested, in particular covering potential failure cases
* **Resilience** Components should be immune against temporary outages, especially in regard to networking and storage
* **Logging** Components should give helpful feedback for during normal operation and especially when errors occur


## Components

* [Shell]({{%relref "shell/_index.md" %}}) Reusable shell functions for infrastructure automation and developer experience
* [Cloud-Init]({{%relref "cloud-init/_index.md" %}}) Based on [Shell]({{%relref "shell/_index.md" %}}) reusable shell functions for typical [Cloud-Init](https://cloudinit.readthedocs.io/en/latest/) user-data usage scenarios
* [RDS]({{%relref "rds/_index.md" %}}) A containerized [PostgreSQL](https://www.postgresql.org/) database with all batteries included backup solution powered by [pgBackRest](https://pgbackrest.org/)
* [Hetzner]({{%relref "hetzner/_index.md" %}}) A collection of infrastructure components for the [Hetzner Cloud](https://cloud.hetzner.de)


## Development

See [Development]({{%relref "development/_index.md" %}}) for hints around developing Solidblocks 