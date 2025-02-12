import os
import re
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


def test_lego(host):
    assert host.file(f"/usr/bin/lego").is_file
    assert host.file(f"/tmp/ssl/certificates/{os.environ['SSL_DOMAIN']}.crt").is_file
    assert host.file(f"/tmp/ssl/certificates/{os.environ['SSL_DOMAIN']}.crt").user == "root"
    assert host.file(f"/tmp/ssl/certificates/{os.environ['SSL_DOMAIN']}.crt").group == "root"
    assert host.file(f"/tmp/ssl/certificates/{os.environ['SSL_DOMAIN']}.crt").mode == 0o600

    assert host.file(f"/tmp/ssl/certificates/{os.environ['SSL_DOMAIN']}.key").is_file
    assert host.file(f"/tmp/ssl/certificates/{os.environ['SSL_DOMAIN']}.key").group == "root"
    assert host.file(f"/tmp/ssl/certificates/{os.environ['SSL_DOMAIN']}.key").user == "root"
    assert host.file(f"/tmp/ssl/certificates/{os.environ['SSL_DOMAIN']}.key").mode == 0o600

    assert host.file(f"/solidblocks/secrets/lego.env").is_file
    lego_env = host.file(f"/solidblocks/secrets/lego.env").content_string

    assert host.service("lego-certificate-renewal.service").is_enabled
    assert host.service("lego-certificate-renewal.timer").is_enabled

    assert len(re.findall(r"^HETZNER_API_KEY=.{32}$", lego_env, re.MULTILINE)) == 1
    assert len(re.findall(r"^HETZNER_HTTP_TIMEOUT=30$", lego_env, re.MULTILINE)) == 1
