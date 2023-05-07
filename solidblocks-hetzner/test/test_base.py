import time


def wait_for_cloud_init(host):
    while not host.file("/run/cloud-init/result.json").exists:
        time.sleep(5)


def test_storge_mounts(host):
    wait_for_cloud_init(host)

    assert host.mount_point(f"/storage/data").exists
    assert host.mount_point(f"/storage/backup").exists
