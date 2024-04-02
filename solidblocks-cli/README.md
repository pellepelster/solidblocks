# Solidblocks CLI

This is a proof-of-concept to validate if it is feasible to replace a custom-made shell script implementing the [do-file pattern](https://pelle.io/posts/developer-experience-do-file/) with a declarative approach.

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
