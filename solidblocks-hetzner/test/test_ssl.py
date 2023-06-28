import time


def test_ssl_certificates(host):
    assert host.file(f"/storage/data/ssl/certificates/test.blcks.de.crt").exists
    assert host.file(f"/storage/data/ssl/certificates/test.blcks.de.key").exists


