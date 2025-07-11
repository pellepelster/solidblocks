import json

from fixtures import *


def test_initial_backup_was_executed(host):
    cmd = host.run("pgbackrest --stanza=test-database1 --output=json info")
    backup_info = json.loads(cmd.stdout)

    assert backup_info[0]['backup'][0]['type'] == "full"
    assert not backup_info[0]['backup'][0]['error']


def test_can_insert_data(conn):
    conn.execute("CREATE TABLE IF NOT EXISTS table1 (id VARCHAR PRIMARY KEY);")
    conn.commit()

    conn.execute(f"INSERT INTO table1 (id) VALUES ('{TEST_ID}') ON CONFLICT (id) DO NOTHING;")
    conn.commit()


def test_extension_postgis(conn):
    res = conn.execute("SELECT extversion FROM pg_extension WHERE extname = 'postgis';")
    assert res.fetchone()[0] == "3.5.2"


def test_extension_pglogical(conn):
    res = conn.execute("SELECT extversion FROM pg_extension WHERE extname = 'pglogical';")
    assert res.fetchone()[0] == "2.4.5"


def test_extension_pgaudit(conn):
    res = conn.execute("SELECT extversion FROM pg_extension WHERE extname = 'pgaudit';")
    assert res.fetchone()[0] == "17.1"


def test_extension_pg_ivm(conn):
    res = conn.execute("SELECT extversion FROM pg_extension WHERE extname = 'pg_ivm';")
    assert res.fetchone()[0] == "1.11"


def test_psql_wrapper(host):
    cmd = host.run("test-database1-psql postgres -c 'SELECT now();'")
    assert cmd.rc == 0


def test_pgbackrest_wrapper(host):
    cmd = host.run("test-database1-pgbackrest info")
    assert cmd.rc == 0
