+++
title = "utils Collection"
+++

**Namespace**: blcks

## Description
your collection description



## Roles

### [md_monitor](https://github.com/pellepelster/solidblocks/tree/main/roles/md_monitor)
Poll status for linux software raids and add them as JSON formatted events to a logfile


## Roles vars

    # [md_monitor](https://github.com/pellepelster/solidblocks/tree/main/roles/md_monitor)
    ## md_monitor Description:
        Poll status for linux software raids and add them as JSON formatted events to a logfile
    

    

    
    
    

### md_monitor Defaults

**These are static variables with lower priority**

#### md_monitor File: [defaults/main.yml](https://github.com/pellepelster/solidblocks/tree/main/roles/md_monitor/defaults/main.yml)
            
            

| Var          | Description | Type         | Value       | Title       |
|--------------|-------------|--------------|-------------|-------------|
| schedule  = git@github.com:pellepelster/solidblocks/blob/main/roles/md_monitor/defaults/main.yml#L4 | execution schedule for software raid status checks | str   | `*-*-* *:*:00` |     systemd schedule |
| logfile  = git@github.com:pellepelster/solidblocks/blob/main/roles/md_monitor/defaults/main.yml#L8 | path for the logfile where the software raid status event will be written to | str   | `/var/log/md_status.log` |     status logfile |
| bin_dir  = git@github.com:pellepelster/solidblocks/blob/main/roles/md_monitor/defaults/main.yml#L12 | installation dir for check script | str   | `/usr/local/bin` |     installation dir for check script |


    

