from os import environ
from pprint import pprint

import pytest
import re


@pytest.mark.parametrize("dir", ["lib", "templates", ""])
def test_solidblocks_dirs(host, dir):
    assert host.file(f"/solidblocks/{dir}").is_directory
    assert host.file(f"/solidblocks/{dir}").user == "solidblocks"
    assert host.file(f"/solidblocks/{dir}").group == "solidblocks"
    assert host.file(f"/solidblocks/{dir}").mode == 0o770
    assert host.file(f"/solidblocks/secrets").mode == 0o700


@pytest.fixture(autouse=True)
def dump_(host):
    yield

    if environ.get('CI') is None:
        print("========== /var/log/cloud-init-output.log ==========")
        print(host.file("/var/log/cloud-init-output.log").content_string)
        print("====================================================")


def test_bootstrap(host):
    assert host.file(f"/solidblocks/lib/storage.sh").is_file
    assert host.file(f"/solidblocks/lib/lego.sh").is_file
    #assert host.file(f"/solidblocks/bin/lego-update-certificate-permissions.sh").is_file


def test_storge_mount(host):
    assert host.mount_point(f"/data1").exists
    assert host.mount_point(f"/data1").filesystem == "ext4"


def test_lego(host):
    assert host.file(f"/usr/bin/lego").is_file
    assert host.file(f"/data1/ssl/certificates/test1.blcks.de.crt").is_file
    assert host.file(f"/data1/ssl/certificates/test1.blcks.de.crt").user == "root"
    assert host.file(f"/data1/ssl/certificates/test1.blcks.de.crt").group == "root"
    assert host.file(f"/data1/ssl/certificates/test1.blcks.de.crt").mode == 0o600

    assert host.file(f"/data1/ssl/certificates/test1.blcks.de.key").is_file
    assert host.file(f"/data1/ssl/certificates/test1.blcks.de.key").user == "root"
    assert host.file(f"/data1/ssl/certificates/test1.blcks.de.key").group == "root"
    assert host.file(f"/data1/ssl/certificates/test1.blcks.de.key").mode == 0o600

    assert host.file(f"/solidblocks/secrets/lego.env").is_file
    lego_env = host.file(f"/solidblocks/secrets/lego.env").content_string

    assert host.service("lego-certificate-renewal.service").is_enabled
    assert host.service("lego-certificate-renewal.timer").is_enabled

    assert len(re.findall(r"^HETZNER_API_KEY=.{32}$", lego_env, re.MULTILINE)) == 1
    assert len(re.findall(r"^HETZNER_HTTP_TIMEOUT=30$", lego_env, re.MULTILINE)) == 1
