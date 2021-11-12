#######################################
# common.sh                           #
#######################################

function package_update {
  apt-get update
}

function package_check_and_install {
	local package=${1}
	echo -n "checking if package '${package}' is installed..."
	if [[ $(dpkg-query -W -f='${Status}' "${package}" 2>/dev/null | grep -c "ok installed") -eq 0 ]];
	then
		echo "not found, installing now"
		while ! DEBIAN_FRONTEND="noninteractive" apt-get install --no-install-recommends -qq -y "${package}"; do
    		echo "installing failed retrying in 30 seconds"
    		sleep 30
    		apt-get update
		done
	else
		echo "ok"
	fi
}

function create_directory_if_needed {
    local directory="${1}"

    if [[ ! -d "${directory}" ]]; then
        mkdir -p "${directory}"
    fi
}

function download_and_verify_checksum {
    local url=${1}
    local target_file=${2}
    local checksum=${3}

    if [[ -f "${target_file}" ]]; then
        local target_file_checksum
        target_file_checksum=$(sha256sum "${target_file}" | cut -d' ' -f1)
        if [[ "${target_file_checksum}" = "${checksum}" ]]; then
            echo "${url} already downloaded"
            return
        fi
    fi

    create_directory_if_needed "$(dirname "${target_file}")"

    echo -n "downloading ${url}..."
    curl_wrapper "${url}" --output "${target_file}" > /dev/null
    echo "done"


    echo -n "verifying checksum..."
    echo "${checksum}" "${target_file}" | sha256sum --check --quiet
    echo "done"
}
