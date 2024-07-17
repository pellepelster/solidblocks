from os import environ
import pytest

@pytest.fixture(autouse=True)
def dump_cloud_init(host):
    yield

    if environ.get('CI') is None:
        print("========== /var/log/cloud-init-output.log ==========")
        print(host.file("/var/log/cloud-init-output.log").content_string)
        print("====================================================")

def test_is_bootstrapped(host):
    assert host.file(f"/solidblocks/lib/storage.sh").is_file
    assert host.file(f"/solidblocks/lib/lego.sh").is_file

@pytest.mark.parametrize("dir", ["lib", "templates", ""])
def test_solidblocks_dirs(host, dir):
    assert host.file(f"/solidblocks/{dir}").is_directory
    assert host.file(f"/solidblocks/{dir}").user == "solidblocks"
    assert host.file(f"/solidblocks/{dir}").group == "solidblocks"
    assert host.file(f"/solidblocks/{dir}").mode == 0o770
    assert host.file(f"/solidblocks/secrets").mode == 0o700

