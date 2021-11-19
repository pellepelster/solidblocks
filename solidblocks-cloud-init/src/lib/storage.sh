function mount_storage() {

    while [ ! -b "[=storage_local_device]" ]; do
      echo "waiting for storage device '[=storage_local_device]'"
      sleep 5
    done

    echo "[=storage_local_device] /storage/local   ext4   defaults  0 0" >> /etc/fstab
    mkdir -p "/storage/local"
    mount "/storage/local"
}
