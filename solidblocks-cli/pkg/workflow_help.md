A Solidblocks workflow can be used to orchestrate the execution of commands in a declarative way, providing support to

* inject environment variables from secret managers
* use output of commands as input for other commands
* define hierarchies of environments variables and configurations

**Example Workflow**

```
providers:
  - pass:
      description: "default pass secret provide"
      path: ${HOME}/.secret_store

environment:
  - name: GLOBAL_VARIABLE1
    value: "foo bar"

tasks:
  - task1:
      shell:
        command: "whoami"
```

**Environment**

Global environment variables can be defined under the `environment` key in the following formats:

***Static variable***

set a global environment variable for all tasks with a static value

```
- name: <name>
  value: "<value>"
```

***Inherited variable***

set a global environment variable for all tasks with a value inherited from the calling environment

```
- name: <name>
```

***Variable from task output***

Use the output of another task as variable conent

```
- name: <name>
  valueFrom:
    task: <name>
```

**Tasks**

The tasks that should be executed have to be registered  under the `tasks` key with a unique name:

```
tasks:
  - <name>:
      description: "[...]"
      <runner>:
          [...]
```

To see a list of the available task runners, run `blcks workflow runners` or `blcks workflow runners <runner>` to get help for a specific runner.