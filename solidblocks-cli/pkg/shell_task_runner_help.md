**Help for task runner `shell`**

A runner to execute shell commands

***Run a command***
```
shell:
  command: "whoami"
```

***Run a command with arguments***
```
shell:
  command: [ "id", "-u" ]
```

***Run a script***

```
shell:
  script: /path/to/script.sh
```

The script must be executable.