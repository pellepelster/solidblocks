function caddy_setup() {
  apt_ensure_package "caddy"

  mkdir -p /storage/data/www
  mkdir -p /storage/data/www/logs/
  chown -R caddy:caddy /storage/data/www

  caddy_config > /etc/caddy/Caddyfile
  systemctl restart caddy
}

function caddy_config() {
  cat <<EOF
{
  storage file_system $${BLCKS_STORAGE_MOUNT_DATA}/www
  email contact@$${DNS_ZONE}
}

$${S3_API_FQDN} {
  log {
    level  INFO

    output file $${BLCKS_STORAGE_MOUNT_DATA}/www/logs/$${S3_API_FQDN}.log {
      roll_uncompressed
      roll_keep     10000
      roll_keep_for 87600h
      roll_size     10MiB
    }
  }

  reverse_proxy http://localhost:3900
}

%{ for s3_bucket in s3_buckets }
${s3_bucket.name}.$${S3_API_FQDN} {
  log {
    level  INFO

    output file $${BLCKS_STORAGE_MOUNT_DATA}/www/logs/${s3_bucket.name}.$${S3_API_FQDN}.log {
      roll_uncompressed
      roll_keep     10000
      roll_keep_for 87600h
      roll_size     10MiB
    }
  }

  reverse_proxy http://localhost:3900
}
%{ if s3_bucket.web_access_public_enable }
${s3_bucket.name}.$${S3_WEB_FQDN} {
  log {
    level  INFO

    output file $${BLCKS_STORAGE_MOUNT_DATA}/www/logs/${s3_bucket.name}.$${S3_WEB_FQDN}.log {
      roll_uncompressed
      roll_keep     10000
      roll_keep_for 87600h
      roll_size     10MiB
    }
  }

  reverse_proxy http://localhost:3902
}

%{ for web_access_domain in s3_bucket.web_access_domains }
${web_access_domain} {
  log {
    level  INFO

    output file $${BLCKS_STORAGE_MOUNT_DATA}/www/logs/${web_access_domain}.log {
      roll_uncompressed
      roll_keep     10000
      roll_keep_for 87600h
      roll_size     10MiB
    }
  }

  reverse_proxy http://localhost:3902
}
%{ endfor }
%{ endif }
%{ endfor }

$${S3_ADMIN_FQDN} {
  log {
    level  INFO

    output file $${BLCKS_STORAGE_MOUNT_DATA}/www/logs/$${S3_ADMIN_FQDN}.log {
      roll_uncompressed
      roll_keep     10000
      roll_keep_for 87600h
      roll_size     10MiB
    }
  }

  reverse_proxy http://localhost:3903
}

%{ if docker_enable }
$${DOCKER_REGISTRY_PRIVATE_FQDN} {

  @write {
      method POST PUT DELETE PATCH DELETE
  }

  @read {
      method GET HEAD OPTIONS
  }

  handle @write {
    basicauth {
      %{ for user in docker_rw_users }
      ${user.username} $(echo ${user.password} | caddy hash-password)
      %{ endfor }
    }
    reverse_proxy http://localhost:5000
  }

  handle @read {
    basicauth {
      %{ for user in docker_ro_users }
      ${user.username} $(echo ${user.password} | caddy hash-password)
      %{ endfor }
      %{ for user in docker_rw_users }
      ${user.username} $(echo ${user.password} | caddy hash-password)
      %{ endfor }
    }
    reverse_proxy http://localhost:5000
  }

  log {
    level  INFO

    output file $${BLCKS_STORAGE_MOUNT_DATA}/www/logs/$${DOCKER_REGISTRY_PRIVATE_FQDN}.log {
      roll_uncompressed
      roll_keep     10000
      roll_keep_for 87600h
      roll_size     10MiB
    }
  }
}

%{ if docker_public_enable }
$${DOCKER_REGISTRY_PUBLIC_FQDN} {

  reverse_proxy http://localhost:5000

  log {
    level  INFO

    output file $${BLCKS_STORAGE_MOUNT_DATA}/www/logs/$${DOCKER_REGISTRY_PUBLIC_FQDN}.log {
      roll_uncompressed
      roll_keep     10000
      roll_keep_for 87600h
      roll_size     10MiB
    }
  }
}
%{ endif }
%{ endif }
EOF
}
