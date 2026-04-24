function github_runner_start_script() {
  local install_dir="${1:-}"
  cat <<EOF
#!/bin/env bash

if [[ ! -f "${install_dir}/.runner" ]]; then
  ${install_dir}/config.sh \
    --url "\${GITHUB_URL}" \
    --token "\${RUNNER_TOKEN}" \
    --name "\${RUNNER_NAME}" \
    --labels "\${RUNNER_LABELS:-}" \
    --unattended
fi
exec ${install_dir}/run.sh
EOF
}

function github_runner_install() {
  local install_dir="/home/github-runner"
  local version="2.333.1"
  local sha256_hash="18f8f68ed1892854ff2ab1bab4fcaa2f5abeedc98093b6cb13638991725cab74"
  useradd -m github-runner --home-dir "${install_dir}"

  curl_wrapper https://github.com/actions/runner/releases/download/v${version}/actions-runner-linux-x64-${version}.tar.gz -o /tmp/actions-runner-linux-x64-${version}.tar.gz
  echo "${sha256_hash}  /tmp/actions-runner-linux-x64-${version}.tar.gz" | sha256sum --check

  tar -xvzf /tmp/actions-runner-linux-x64-${version}.tar.gz -C "${install_dir}"
  rm -f /tmp/actions-runner-linux-x64-${version}.tar.gz
  "${install_dir}/bin/installdependencies.sh"

  github_runner_start_script ${install_dir} > "${install_dir}/start_runner.sh"
  chmod +x "${install_dir}/start_runner.sh"
  chown -R github-runner:github-runner "${install_dir}"
}
