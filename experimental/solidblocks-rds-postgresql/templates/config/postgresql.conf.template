archive_command = 'pgbackrest --config /rds/config/pgbackrest.conf --log-level-console=info --log-path=/rds/log --stanza={{ .Env.DB_DATABASE }} archive-push  %p'
archive_mode = on
max_wal_senders = 3
wal_level = replica
listen_addresses = '*'
unix_socket_directories =  '/rds/socket'
