<!-- DOCSIBLE START -->

# 📃 Role overview

## md_monitor

```
Role belongs to blcks/utils
Namespace - blcks
Collection - utils
Version - 0.0.0-snapshot
Repository - https://github.com/pellepelster/solidblocks
```

Description: Poll status for linux software raids and add them as JSON formatted events to a logfile







<details>
<summary><b>🧩 Argument Specifications in meta/argument_specs</b></summary>

#### Key: main
**Description**: setup PostgreSQL database


  - **instance_name**
    - **Required**: True
    - **Type**: 
    - **Default**: none
    - **Description**: database instance name
  
  
  

  - **environment_name**
    - **Required**: True
    - **Type**: 
    - **Default**: none
    - **Description**: environment name
  
  
  

  - **postgres_version**
    - **Required**: False
    - **Type**: 
    - **Default**: none
    - **Description**: postgres version to install
  
  
  

  - **superuser_username**
    - **Required**: False
    - **Type**: 
    - **Default**: none
    - **Description**: database superuser name
  
  
  

  - **superuser_password**
    - **Required**: True
    - **Type**: 
    - **Default**: none
    - **Description**: database superuser password
  
  
  

  - **backup_password**
    - **Required**: True
    - **Type**: 
    - **Default**: none
    - **Description**: pgbackrest encryption password
  
  
  

  - **extension_pglogical_enabled**
    - **Required**: False
    - **Type**: 
    - **Default**: none
    - **Description**: enable pglogical postgres extension
  
  
  

  - **extension_postgis_enabled**
    - **Required**: False
    - **Type**: 
    - **Default**: none
    - **Description**: enable postgis postgres extension
  
  
  

  - **extension_pg_ivm_enabled**
    - **Required**: False
    - **Type**: 
    - **Default**: none
    - **Description**: enable postgis postgres extension
  
  
  

  - **extension_pgvector_enabled**
    - **Required**: False
    - **Type**: 
    - **Default**: none
    - **Description**: enable postgis postgres extension
  
  
  

  - **extension_pgaudit_enabled**
    - **Required**: False
    - **Type**: 
    - **Default**: none
    - **Description**: enable pgaudit postgres extension
  
  
  

  - **backup_s3_endpoint**
    - **Required**: True
    - **Type**: 
    - **Default**: none
    - **Description**: TODO
  
  
  

  - **backup_s3_bucket**
    - **Required**: True
    - **Type**: 
    - **Default**: none
    - **Description**: TODO
  
  
  

  - **backup_s3_key**
    - **Required**: True
    - **Type**: 
    - **Default**: none
    - **Description**: TODO
  
  
  

  - **backup_s3_key_secret**
    - **Required**: True
    - **Type**: 
    - **Default**: none
    - **Description**: TODO
  
  
  

  - **backup_s3_region**
    - **Required**: True
    - **Type**: 
    - **Default**: none
    - **Description**: TODO
  
  
  

  - **backup_s3_uri_style**
    - **Required**: False
    - **Type**: 
    - **Default**: none
    - **Description**: TODO
  
  
  



</details>




### Defaults

**These are static variables with lower priority**

#### File: defaults/main.yml

| Var          | Type         | Value       | Title       |
|--------------|--------------|-------------|-------------|
| [schedule](git@github.com:pellepelster/solidblocks/blob/main/roles/md_monitor/defaults/main.yml#L4)   | str   | `*-*-* *:*:00` |     systemd schedule |
| [logfile](git@github.com:pellepelster/solidblocks/blob/main/roles/md_monitor/defaults/main.yml#L8)   | str   | `/var/log/md_status.log` |     status logfile |
| [bin_dir](git@github.com:pellepelster/solidblocks/blob/main/roles/md_monitor/defaults/main.yml#L12)   | str   | `/usr/local/bin` |     installation dir for check script |
<details>
<summary><b>🖇️ Full descriptions for vars in defaults/main.yml</b></summary>
<br>
<b>schedule:</b> execution schedule for software raid status checks
<br>
<b>logfile:</b> path for the logfile where the software raid status event will be written to
<br>
<b>bin_dir:</b> installation dir for check script
<br>
<br>
</details>





### Tasks


#### File: tasks/main.yml

| Name | Module | Has Conditions |
| ---- | ------ | --------- |
| install packages | ansible.builtin.package | False |
| install md-monitor.sh | ansible.builtin.template | False |
| create md-monitor systemd service | ansible.builtin.template | False |
| create md-monitor systemd timer | ansible.builtin.template | False |
| enable md-monitor systemd timer | ansible.builtin.systemd_service | False |







## Author Information
Unknown Author

#### License

No license specified.

#### Minimum Ansible Version

No minimum version specified.

#### Platforms

- **Debian**: ['buster', 'bullseye']
- **Ubuntu**: ['bionic']


#### Dependencies

No dependencies specified.
<!-- DOCSIBLE END -->
