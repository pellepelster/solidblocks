function mount_storage() {

    while [ ! -b "${SOLIDBLOCKS_STORAGE_LOCAL_DEVICE}" ]; do
      echo "waiting for storage device '${SOLIDBLOCKS_STORAGE_LOCAL_DEVICE}'"
      sleep 5
    done

    echo "${SOLIDBLOCKS_STORAGE_LOCAL_DEVICE} ${SOLIDBLOCKS_STORAGE_LOCAL_DIR}   ext4   defaults  0 0" >> /etc/fstab
    mkdir -p "${SOLIDBLOCKS_STORAGE_LOCAL_DIR}"
    mount "${SOLIDBLOCKS_STORAGE_LOCAL_DIR}"
}
