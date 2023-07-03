import time


def test_ssl_certificates(host):
    assert host.file(f"/storage/data/ssl/certificates/test3.blcks.de.crt").exists
    assert host.file(f"/storage/data/ssl/certificates/test3.blcks.de.key").exists


