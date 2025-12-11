function garage_config() {
  cat <<EOF
metadata_dir = "$${BLCKS_STORAGE_MOUNT_DATA}/garage/meta"
data_dir = "$${BLCKS_STORAGE_MOUNT_DATA}/garage/data"
db_engine = "sqlite"

replication_factor = 1

rpc_bind_addr = "[::]:3901"
rpc_public_addr = "127.0.0.1:3901"
rpc_secret = "${rpc_secret}"

[s3_api]
s3_region = "garage"
api_bind_addr = "[::]:3900"
root_domain = "$${S3_API_FQDN}"

[s3_web]
bind_addr = "[::]:3902"
root_domain = "$${S3_WEB_FQDN}"
index = "index.html"

[k2v_api]
api_bind_addr = "[::]:3904"

[admin]
api_bind_addr = "[::]:3903"
admin_token = "${admin_token}"
metrics_token = "${metrics_token}"
EOF
}

function garage_provision() {
  apt_ensure_package "python3-venv"
  apt_ensure_package "git"
  export ADMIN_ADDRESS="http://localhost:3903"
  export ADMIN_TOKEN="${admin_token}"

  (
    cd /root
    python3 -m venv ".venv"
    .venv/bin/pip install -r "requirements.txt"
    .venv/bin/python "garage.py" s3_buckets.json
  )
}