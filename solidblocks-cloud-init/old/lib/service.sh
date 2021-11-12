function service_ensure_user() {
    local user
    user="service"
    local group
    group="service"
    if [[ ! $(id -u "${user}") ]]; then
        groupadd -g 4000 "${group}"
        useradd --no-create-home --uid 4000 --gid 4000 --no-user-group "${user}"
    fi
}
