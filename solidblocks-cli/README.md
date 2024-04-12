# Solidblocks CLI

`soliblocks-cli` (henceforth called `blcks`) is a proof-of-concept to validate if it is feasible to replace a custom-made shell script implementing the [do-file pattern](https://pelle.io/posts/developer-experience-do-file/) with a declarative approach.

Based on the real-life implementations of the do-file pattern I have seen in the past the design goal is to move patterns that are always the same into a declarative configuration file reducing the amount of scaffolding and glue code that is needed everytime.

## Goals

* have an easy to understand (tm) declarative configuration file

TODO why yaml

Although having the whole build/test/deploy cycle as a shell or python script is already a big step, it can quickly become very tedious to model dependencies between tasks, make sure the task descriptions

* orchestrate calls for commands
* allow to model dependencies between commands
* have a way to define environment variables, either globally or per command
* allow to pass output of commands between tasks

## Non-Goals

* replace fully fledged Ci configurations like e.g. `.gitlab-ci.yml`




POC Features:

* execute shell tasks in a defined order
 * use output from task a as variable in task b
  * support transformations for outputs
    * jq
    * regex
* Inject secrets from secret manager
* define and inject hierarchy of environment variables
* preflight check workflow
* detailed logging for workflow
