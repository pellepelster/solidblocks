---
title: systemd example
weight: 100
---

Below is an example how to use systemd to start a docker-compose based Solidblocks RDS instances, together with timers, to schedule backups.

`/opt/dockerfiles/instance1/docker-compose.yml`
```yaml
version: "3"
services:
  instance1:
    image: ghcr.io/pellepelster/solidblocks-rds-postgresql:17-v{{% env "SOLIDBLOCKS_VERSION" %}}
    environment:
      - "DB_INSTANCE_NAME=instance1"
      - "DB_..." # see https://pellepelster.github.io/solidblocks/rds/index.html#configuration
    ports:
      - "5432:5432"
    volumes:
      - "/storage/data:/storage/data"
      - "/storage/backup:/storage/backup"
```

`/etc/systemd/system/rds@.service`
```shell
[Unit]
Description=rds instance %i
Requires=docker.service
After=docker.service

[Service]
Restart=always
RestartSec=10s

WorkingDirectory=/opt/dockerfiles/%i
ExecStartPre=/usr/bin/docker-compose down -v
ExecStartPre=/usr/bin/docker-compose rm -fv
ExecStartPre=/usr/bin/docker-compose pull

ExecStart=/usr/bin/docker-compose up
ExecStop=/usr/bin/docker-compose down -v

[Install]
WantedBy=multi-user.target
```

`/etc/systemd/system/rds-backup-<full|incr>@.service`
```shell
[Unit]
Description=<full|incr> backup for %i

[Service]
Type=oneshot
WorkingDirectory=/opt/dockerfiles/%i
ExecStart=/usr/bin/docker-compose exec -T instance1 /rds/bin/backup-<full|incr>.sh
```

`/etc/systemd/system/rds-backup-<full|incr>@.timer`
```shell
[Unit]
Description=<full|incr> backup for %i

[Timer]
OnCalendar=00 00 * * *

Unit=rds-backup-<full|incr>@%i.service

[Install]
WantedBy=multi-user.target
```

**enable system units**
```shell
systemctl daemon-reload
systemctl enable rds@instance1
systemctl start rds@instance1

systemctl enable rds-backup-<full|incr>@instance1.timer
systemctl start rds-backup-<full|incr>@instance1.timer
systemctl enable rds-backup-<full|incr>@instance1.timer
systemctl start rds-backup-<full|incr>@instance1.timer
```
