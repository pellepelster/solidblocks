+++
title = "Naming Conventions"
description = "Infrastructure naming conventions"
+++

In the scope of Solidblocks components, naming conventions for infrastructure resources serve two main purposes. One is
to ensure a unique, consistent and distinctive naming of created resources. The other one is to ensure, that different
resources are
able to reference each other and play together nicely.

Resource in this context is used as an abstract term for any piece of virtual (SaaX) or physical infrastructure that is
created or used, e.g., servers, load-balancers, databases, firewall rules, users, roles, and so on.

# Resource Identifiers

The name of the resource itself typically is a concatenation of multiple of the following identifiers

| identifier  | description                                                                                                | examples                        |
|-------------|------------------------------------------------------------------------------------------------------------|---------------------------------|
| name        | The name of the application or solution                                                                    | `k3s`, `wordpress`              |
| namespace   | A preferably short (~5 letters) abbreviation of the company, project or organizational unit                | `project-x`, `accounting`, `hr` |
| tenant      | A customer identifier, indicating who this instance of a resource is for                                   | `initech`, `acme`               |
| region      | For multi region deployments this identifies the regions the resource is associated to                     | `eu`, `apac`, `asia`            |
| environment | Stage the resource belongs to                                                                              | `prod`, `dev`                   |
| component   | Larger solutions can be composed of different components                                                   | `database`, `webserver`         |
| technology  | Sometimes it can be helpful to directly see the underlying technology of a resource                        | `postgresql`, `nginx`           |
| channel     | In the case of multiple development streams, the name of the stream the resource was created from          | `nightly`, `stable`             |
| instance_id | A unique id that can be use to make instance of an application, typically referred to by the `name` unique | `k3s-z5KyMg`, `postgres-UIf2IS` |
| index       | A counter designating the sequential number of a resource with multiple instances                          | `k3s-1`, `postgres-002`         |

The easiest strategy for naming resources would we to slap together enough uniquely identifiable attributes until
collisions are not possible anymore. While this might technically work, we have to take some considerations into
account:

* Is there a limit on the maximum length for resource names?
* Do we need to use resource names that can be understood by a human? A random UUID might be safe and easy to use for
  computers, but tedious for humans who need to interact with resources named like this
* We might want to make critical resources clearly visible, e.g., it should always be clear what environment resources
  belong to (prod, dev, ...)
* Is global uniqueness and collision safety a consideration we need to take into account? e.g., AWS S3 bucket names need
  to be globally unique
* How do we concatenate the identifiers typically `-` is a good first approach, but depending on the context and
  involved technologies, a resource name that contains a `-` and is passed through Python might cause issues when
  accessing deserialized YAMl/JSON structures
* It is a good practice to order the identifiers in descending scope/significance to form a hierarchy that can easily be
  navigated, `<environment>-<namespace>-<name>`
* Resources can be contained in resource containers that many cloud providers offer to group resources together. This
  could be an account in AWS, a project inside Hetzner Cloud, or a resource group in Azure. In that case the resource
  unique name could be split up e.g. `<environment>` for the account and `<namespace>-<name>` for the resource

## Examples

* A single application with different environments
    * `<environment>-<name>`
        * `dev-website`
        * `prod-website`
* A stateless application with multiple ephemeral instances with different environments
    * `<environment>-<name>-<instance_id>`
        * `prod-website-wd4SUp`
        * `prod-website-z5KyMg`
* A multi-component application with different environments
    * `<environment>-<name>-<component>`
        * `prod-website-database`
        * `prod-website-webserver`
* A stateful application in two different environments
    * `<environment>-<name>-<index>`
        * `dev-broker-1`
        * `dev-broker-2`
* An application with different environments using AWS accounts
    * account `<environment>`/resource `<name>`
        * `dev`/`website`
        * `prod`/`website`

# Metadata

Often created resources support metadata, which in most cases are labels in the form of key/value pairs. Following
establish K8S standards for labeling, the following best practices are a good starting point for choosing label names:

* Valid label keys have two segments: an optional prefix and name, separated by a slash (/).
* The name segment is required and must be 63 characters or less, beginning and ending with an alphanumeric
  character ([a-z0-9A-Z]) with dashes (-), underscores (_), dots (.), and alphanumerics between. The prefix is optional.
* If specified, the prefix must be a DNS subdomain: a series of DNS labels separated by dots (.), not longer than 253
  characters in total, followed by a slash (/).
* If the prefix is omitted, the label Key is presumed to be private to the resource

**examples**

| key                 | description                                                                          | example      | type   |
|---------------------|--------------------------------------------------------------------------------------|--------------|--------|
| blcks.de/name       | The name of the application                                                          | mysql        | string |
| blcks.de/instance   | A unique name identifying the instance of an application                             | mysql-abcxyz | string |
| blcks.de/version    | The current version of the application (e.g., a SemVer version, revision hash, etc.) | 5.7.21       | string |
| blcks.de/component  | The component within the architecture                                                | database     | string |
| blcks.de/part-of    | The name of a higher level application this one is part of                           | wordpress    | string |
| blcks.de/managed-by | The tool being used to manage the operation of an application                        | Helm         | string | 

