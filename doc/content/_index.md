---
title: "Solidblocks"
---

# Solidblocks

Solidblocks is a library of reusable components for infrastructure automation. 

## Design Goals

* **Easy Reuse** Each component should easily be embeddable into any setup without requiring any specific project setup or structure
* **Tested** Components should be tested, in particular covering potential failure cases
* **Resilience** Components should be immune against temporary outages, especially in regard to networking and storage
* **Logging** Components should give helpful feedback for during normal operation and especially when errors occur


## Components

* [Shell]({{%relref "shell/_index.md" %}}) Reusable shell functions for infrastructure automation
* [RDS]({{%relref "rds/_index.md" %}}) A containerized [PostgreSQL](https://www.postgresql.org/) database with all batteries included backup solution powered by [pgBackRest](https://pgbackrest.org/)
