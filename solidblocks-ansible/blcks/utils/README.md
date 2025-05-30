<!-- DOCSIBLE START -->

# 📃 Collection overview

**Namespace**: blcks

**Name**: utils

**Version**: 0.0.0-snapshot

**Authors**:

- Christian 'Pelle' Pelster <pelle@pelle.io>

## Description

your collection description



## Roles

### [md_monitor](https://github.com/pellepelster/solidblocks/tree/main/roles/md_monitor)

## Roles vars

# [md_monitor](https://github.com/pellepelster/solidblocks/tree/main/roles/md_monitor)
## md_monitor Description:
Poll status for linux software raids and add them as JSON formatted events to a logfile






### md_monitor Defaults

**These are static variables with lower priority**

#### md_monitor File: [defaults/main.yml](https://github.com/pellepelster/solidblocks/tree/main/roles/md_monitor/defaults/main.yml)

| Var          | Type         | Value       | Title       |
|--------------|--------------|-------------|-------------|
| [schedule](git@github.com:pellepelster/solidblocks/blob/main/roles/md_monitor/defaults/main.yml#L4)   | str   | `*-*-* *:*:00` |     systemd schedule |
| [logfile](git@github.com:pellepelster/solidblocks/blob/main/roles/md_monitor/defaults/main.yml#L8)   | str   | `/var/log/md_status.log` |     status logfile |
| [bin_dir](git@github.com:pellepelster/solidblocks/blob/main/roles/md_monitor/defaults/main.yml#L12)   | str   | `/usr/local/bin` |     installation dir for check script |
<details>
<summary><b>🖇️ md_monitor Full descriptions for vars in defaults/main.yml</b></summary>
<br>
<b>schedule:</b> execution schedule for software raid status checks
<br>
<b>logfile:</b> path for the logfile where the software raid status event will be written to
<br>
<b>bin_dir:</b> installation dir for check script
<br>
<br>
</details>




## Metadata

- **Repository**: [Repository](https://github.com/pellepelster/solidblocks)

- **Documentation**: [Documentation](https://pellepelster.github.io/solidblocks/)

- **Homepage**: [Homepage](https://github.com/pellepelster/solidblocks)

- **Issues**: [Issues](https://github.com/pellepelster/solidblocks/issues)

<!-- DOCSIBLE END -->
