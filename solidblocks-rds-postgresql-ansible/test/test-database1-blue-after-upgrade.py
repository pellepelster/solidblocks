from fixtures import *


def test_data_still_there(conn):
    res = conn.execute(f"SELECT * FROM table1 WHERE id = '{TEST_ID}';")
    assert res.fetchone()[0] == TEST_ID


def test_version(conn):
    res = conn.execute("SELECT version();")
    assert res.fetchone()[0].startswith("PostgreSQL 17")
