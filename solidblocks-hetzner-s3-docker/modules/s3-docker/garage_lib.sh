function garage_systemd_unit() {
  cat <<EOF
[Unit]
Description=Garage Data Store
After=network-online.target
Wants=network-online.target

[Service]
Environment='RUST_LOG=garage=info' 'RUST_BACKTRACE=1'
ExecStart=/usr/local/bin/garage server
StateDirectory=garage
#DynamicUser=true
#ProtectHome=true
#NoNewPrivileges=true
LimitNOFILE=42000

[Install]
WantedBy=multi-user.target
EOF
}

function garage_setup() {
  local size="${1:-1}"
  curl -L https://garagehq.deuxfleurs.fr/_releases/v2.1.0/x86_64-unknown-linux-musl/garage -o /usr/local/bin/garage
  echo "543b0414d1464ab855ebe9b843938a5e5361fd24436891ce3dff9e03d02839d8 /usr/local/bin/garage" | sha256sum --check
  chmod +x /usr/local/bin/garage
  garage_systemd_unit > /etc/systemd/system/garage.service
  systemctl daemon-reload

  garage_config > /etc/garage.toml
  mkdir -p ${BLCKS_STORAGE_MOUNT_DATA}/garage/meta
  mkdir -p ${BLCKS_STORAGE_MOUNT_DATA}/garage/data
  systemctl start garage

  while ! garage status; do
    echo "waiting for garage startup"
    sleep 5
  done

  local node_id="$(garage status | grep '127.0.0.1' | awk '{print $1}')"
  garage layout assign -z dc1 -c "${size}G" "${node_id}"
  local apply_command="$(garage layout show | grep 'garage layout apply')"
  ${apply_command}
}

function garage_deny_keys_by_name() {
    local key_name="${1:-cloud-init}"
    local keys_to_deny="$(garage key list | grep ${key_name} | awk '{print $1}')"
    local buckets="$(garage bucket list | tail -n +2 | awk '{print $3}')"

    for key_to_deny in ${keys_to_deny}; do
      echo "revoking access for key '${key_to_deny}'"
      garage key deny "${key_to_deny}"

      for bucket in ${buckets}; do
        garage bucket deny "${bucket}" --key "${key_to_deny}" --owner --read --write
      done
    done
}

function garage_ensure_bucket() {
    local bucket="${1:-}"
    if ! garage bucket info "${bucket}"; then
      echo "creating bucket '${bucket}'"
      garage bucket create "${bucket}"
    else
      echo "bucket '${bucket}' already exists"
    fi
}


function garage_ensure_key() {
    local key_name="${1:-}"
    local key_id="${2:-}"
    local secret_key="${3:-}"

    if ! garage key info "${key_id}"; then
      echo "importing key '${key_id}'"
      garage key import "${key_id}" "${secret_key}" -n "${key_name}" --yes
    else
      echo "key '${key_id}' already exists"
    fi
}

function garage_bucket_ensure_owner() {
    local bucket="${1:-}"
    local key_id="${2:-}"
    garage bucket allow "${bucket}" --key ${key_id} --owner --read --write
}

function garage_bucket_ensure_ro() {
    local bucket="${1:-}"
    local key_id="${2:-}"
    garage bucket allow "${bucket}" --key ${key_id} --read
}

function garage_bucket_enable_website() {
    local bucket="${1:-}"
    echo "enabling website access for bucket '${bucket}'"
    garage bucket website "${bucket}" --allow
}

function garage_bucket_disable_website() {
    local bucket="${1:-}"
    echo "disabling website access for bucket '${bucket}'"
    garage bucket website "${bucket}" --deny
}
