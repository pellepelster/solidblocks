#######################################
# docker.sh                           #
#######################################

function install_docker() {
    curl_wrapper https://download.docker.com/linux/debian/gpg | apt-key add -
    add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable"
    apt-get update
    check_and_install "docker-ce"
    docker_daemon_config > /etc/docker/daemon.json
    service docker restart
}

function docker_login() {
    config '.registry_password' | docker login --username "$(config '.registry_username')" --password-stdin registry.gitlab.com
}

function docker_daemon_config {
cat <<-EOF
{
  "dns": $(hetzner_controller_private_ips)
}
EOF
}

function bootstrap_docker_manager() {
    check_and_install "apt-transport-https"
    check_and_install "ca-certificates"
    check_and_install "curl"
    check_and_install "jq"
    check_and_install "gnupg2"
    check_and_install "software-properties-common"
    install_docker
    docker_login

    if [ "${DOCKER_LISTEN_LOCALHOST}" -gt 0 ]; then
        sed -i 's/^ExecStart.*$/ExecStart=\/usr\/bin\/dockerd -H unix:\/\/ -H tcp:\/\/127.0.0.1:2375/' /lib/systemd/system/docker.service
        systemctl daemon-reload
        systemctl restart docker
    fi
}
 
function bootstrap_docker_worker() {
    check_and_install "apt-transport-https"
    check_and_install "ca-certificates"
    check_and_install "curl"
    check_and_install "jq"
    check_and_install "gnupg2"
    check_and_install "software-properties-common"
    install_docker
    docker_login
}
 
