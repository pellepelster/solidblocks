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
