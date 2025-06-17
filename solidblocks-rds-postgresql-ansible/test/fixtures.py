import os

import psycopg
import pytest

TEST_ID = os.getenv("TEST_ID")


@pytest.fixture(scope="module", autouse=True)
def ip_addr(host):
    cmd = host.run("ip addr show eth0 | grep 'inet' | awk '{print $2}' | head -1 | cut -d/ -f1")
    assert cmd.rc == 0
    return cmd.stdout.strip()


@pytest.fixture(scope="module", autouse=True)
def conn(ip_addr):
    return psycopg.Connection.connect(f"dbname=postgres user=rds password=yolo2000 host={ip_addr} port=5432")
