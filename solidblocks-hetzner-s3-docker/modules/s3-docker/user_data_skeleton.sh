#!/usr/bin/env bash

set -eu -o pipefail

${variables}
${storage_lib}
${apt_lib}
${caddy_lib}
${caddy}
${garage}
${garage_lib}
${docker_registry_lib}

storage_mount "$${BLCKS_STORAGE_DEVICE_DATA}" "$${BLCKS_STORAGE_MOUNT_DATA}"

apt_update_repositories
caddy_setup
garage_setup "${data_volume_size}"

garage_deny_keys_by_name "cloud-init"

%{ for s3_bucket in s3_buckets }
garage_ensure_bucket "${s3_bucket.name}"
%{ if s3_bucket.enable_public_web_access == true }
garage_bucket_enable_website "${s3_bucket.name}"
%{ else }
garage_bucket_disable_website "${s3_bucket.name}"
%{ endif }
%{ endfor }

%{ for s3_bucket in s3_buckets }
garage_ensure_key "cloud-init" "${s3_bucket.owner_key_id}" "${s3_bucket.owner_secret_key}"

garage_bucket_ensure_owner "${s3_bucket.name}" "${s3_bucket.owner_key_id}"

%{ if  s3_bucket.ro_key_id != null && s3_bucket.ro_secret_key != null }
garage_ensure_key "cloud-init" "${s3_bucket.ro_key_id}" "${s3_bucket.ro_secret_key}"
garage_bucket_ensure_ro "${s3_bucket.name}" "${s3_bucket.ro_key_id}"
%{ endif }
%{ endfor }

docker_registry_setup
