import json

from fixtures import *


def test_initial_backup_was_executed(host):
    cmd = host.run("pgbackrest --stanza=test-database2 --output=json info")
    backup_info = json.loads(cmd.stdout)

    assert backup_info[0]['backup'][0]['type'] == "full"
    assert not backup_info[0]['backup'][0]['error']


def test_can_insert_data(conn):
    conn.execute("CREATE TABLE IF NOT EXISTS table1 (id VARCHAR PRIMARY KEY);")
    conn.commit()

    conn.execute(f"INSERT INTO table1 (id) VALUES ('{TEST_ID}') ON CONFLICT (id) DO NOTHING;")
    conn.commit()


def test_psql_wrapper(host):
    cmd = host.run("test-database2-psql postgres -c 'SELECT now();'")
    assert cmd.rc == 0


def test_pgbackrest_wrapper(host):
    cmd = host.run("test-database2-pgbackrest info")
    assert cmd.rc == 0


def test_is_master(conn):
    res = conn.execute("SELECT * FROM pg_is_in_recovery();")
    assert res.fetchone()[0] == False
