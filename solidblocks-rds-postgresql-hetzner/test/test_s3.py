import time


def test_storge_mounts(host):
    assert host.mount_point(f"/storage/data").exists
