import time

import pytest


def wait_for_cloud_init(host):
    while not host.file("/run/cloud-init/result.json").exists:
        time.sleep(5)


@pytest.mark.parametrize("dir", ["lib", "templates", ""])
def test_solidblocks_dirs(host, dir):
    wait_for_cloud_init(host)

    assert host.file(f"/solidblocks/{dir}").is_directory
    assert host.file(f"/solidblocks/{dir}").user == "solidblocks"
    assert host.file(f"/solidblocks/{dir}").group == "solidblocks"
    assert host.file(f"/solidblocks/{dir}").mode == 0o770


def test_bootstrap(host):
    wait_for_cloud_init(host)
    assert host.file(f"/solidblocks//lib/storage.sh").is_file


def test_storge_mount(host):
    wait_for_cloud_init(host)

    assert host.mount_point(f"/data1").exists
    assert host.mount_point(f"/data1").filesystem == "ext4"
