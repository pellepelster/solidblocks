from os import environ
import pytest


@pytest.fixture(autouse=True)
def dump_cloud_init(host):
    yield

    if environ.get('CI') is None:
        print("========== /var/log/cloud-init-output.log ==========")
        print(host.file("/var/log/cloud-init-output.log").content_string)
        print("====================================================")


def test_storage_mounts(host):
    assert host.mount_point(f"/data1").exists
    assert host.mount_point(f"/data1").filesystem == "ext4"
    assert host.mount_point(f"/data2").exists
    assert host.mount_point(f"/data2").filesystem == "ext4"
