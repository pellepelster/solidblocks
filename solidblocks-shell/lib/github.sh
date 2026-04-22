function github_runner_install() {
  local install_dir="/home/github-runner"
  local version="2.333.1"
  local sha256_hash="18f8f68ed1892854ff2ab1bab4fcaa2f5abeedc98093b6cb13638991725cab74"
  useradd -m github-runner --home-dir "${install_dir}"

  curl_wrapper https://github.com/actions/runner/releases/download/v${version}/actions-runner-linux-x64-${version}.tar.gz -o /tmp/actions-runner-linux-x64-${version}.tar.gz
  echo "${sha256_hash}  /tmp/actions-runner-linux-x64-${version}.tar.gz" | sha256sum --check

  tar -xvzf /tmp/actions-runner-linux-x64-${version}.tar.gz -C "${install_dir}"
  rm -f /tmp/actions-runner-linux-x64-${version}.tar.gz
  chown -R github-runner:github-runner "${install_dir}"
  "${install_dir}/bin/installdependencies.sh"
}
