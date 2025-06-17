import json
import os

import psycopg
import pytest

from fixtures import conn, ip_addr, TEST_ID

@pytest.fixture(scope="module", autouse=True)
def ip_addr(host):
    cmd = host.run("ip addr show eth0 | grep 'inet' | awk '{print $2}' | head -1 | cut -d/ -f1")
    assert cmd.rc == 0
    return cmd.stdout.strip()


@pytest.fixture(scope="module", autouse=True)
def conn(ip_addr):
    return psycopg.Connection.connect(f"dbname=postgres user=rds password=yolo2000 host={ip_addr} port=5432")


def test_initial_backup_was_executed(host):
    cmd = host.run("pgbackrest --stanza=test-database1 --output=json info")
    backup_info = json.loads(cmd.stdout)

    assert backup_info[0]['backup'][0]['type'] == "full"
    assert not backup_info[0]['backup'][0]['error']


def test_can_insert_data(host, conn):
    conn.execute("CREATE TABLE IF NOT EXISTS table1 (id VARCHAR PRIMARY KEY);")
    conn.commit()

    conn.execute(f"INSERT INTO table1 (id) VALUES ('{TEST_ID}') ON CONFLICT (id) DO NOTHING;")
    conn.commit()
