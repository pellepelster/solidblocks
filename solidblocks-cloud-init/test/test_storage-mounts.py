from os import environ

import pytest

from fixtures import host_is_initialized, wait_until


@pytest.fixture(autouse=True)
def dump_cloud_init(host):
    wait_until(lambda: host_is_initialized(host), 60)

    if environ.get('CI') is None:
        print("========== /var/log/cloud-init-output.log ==========")
        print(host.file("/var/log/cloud-init-output.log").content_string)
        print("====================================================")


def test_storage_mounts(host):
    assert host.mount_point(f"/data1").exists
    assert host.mount_point(f"/data1").filesystem == "ext4"
    assert host.mount_point(f"/data2").exists
    assert host.mount_point(f"/data2").filesystem == "ext4"
