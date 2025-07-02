+++
title = "Naming Conventions"
description = "Infrastructure naming conventions"
+++

Naming conventions for infrastructure resources serve two main purposes. One is to ensure a unique, consistent and
distinctive
naming of created resources. The other one is to ensure with correct labeling that different resources are able to
reference each other, and play together nicely.

Resource in this context is used as an abstract term for any piece of virtual (SaaX) or physical infrastructure that is
created or used, e.g. servers, load-balancers, databases, firewall rules, users, roles, and so on.

# Identifiers

The name of the resource itself, typically is a concatenation of multiple of the following identifiers

| identifier  | description                                                                                       | examples                        |
|-------------|---------------------------------------------------------------------------------------------------|---------------------------------|
| name        | The name of the application or solution                                                           | `k3s`, `postgresql`             |
| namespace   | A preferably short (~5 letters) abbreviation of the company or organizational unit                | `accounting`, `hr`              |
| tenant      | A customer identifier, indicating who this instance of a resource is for                          | `initech`, `acme`               |
| region      | For multi region deployments this identifies the regions the resource is associated to            | `eu`, `apac`, `asia`            |
| environment | Stage the resource belongs to                                                                     | `prod`, `dev`                   |
| channel     | In the case of multiple development streams, the name of the stream the resource was created from | `nightly`, `stable`             |
| instance    | A unique name identifying the instance of an application, typically refering to the `name`        | `k3s-z5KyMg`, `postgres-UIf2IS` |
| index       | A counter designating the sequential number of a resource with multiple instances                 | `k3s-1`, `postgres-002`         |

{{% notice style="caution"  %}}
Make sure you are not using internal identifiers from external systems or APIs that are subject to changes.
{{% /notice %}}

_ -

uniqie

length

# Resource Naming

# Namespace

, to ensure globally unique IDs
blcks.de

* labeling usecases
* dashboards -> components
*

# Metadata

Labels are key/value pairs. Valid label keys have two segments: an optional prefix and name, separated by a slash (/).
The name segment is required and must be 63 characters or less, beginning and ending with an alphanumeric
character ([a-z0-9A-Z]) with dashes (-), underscores (_), dots (.), and alphanumerics between. The prefix is optional.
If specified, the prefix must be a DNS subdomain: a series of DNS labels separated by dots (.), not longer than 253
characters in total, followed by a slash (/).

If the prefix is omitted, the label Key is presumed to be private to the user. Automated system components (e.g.
kube-scheduler, kube-controller-manager, kube-apiserver, kubectl, or other third-party automation) which add labels to
end-user objects must specify a prefix.

The kubernetes.io/ and k8s.io/ prefixes are reserved for Kubernetes core components.

Valid label value:

    must be 63 characters or less (can be empty),
    unless empty, must begin and end with an alphanumeric character ([a-z0-9A-Z]),
    could contain dashes (-), underscores (_), dots (.), and alphanumerics between.

For example, here's a manifest for a Pod that has two labels environment: production and app: nginx:

prefix = namespace

| key                 | description                                                                      | example      | type   
|---------------------|----------------------------------------------------------------------------------|--------------|--------|
| blcks.de/name       | The name of the application                                                      | mysql        | string |
| blcks.de/instance   | A unique name identifying the instance of an application                         | mysql-abcxyz | string |
| blcks.de/version    | The current version of the application (e.g., a SemVer 1.0, revision hash, etc.) | 5.7.21       | string |
| blcks.de/component  | The component within the architecture                                            | database     | string |
| blcks.de/part-of    | The name of a higher level application this one is part of                       | wordpress    | string |
| blcks.de/managed-by | The tool being used to manage the operation of an application                    | Helm         | string | 

