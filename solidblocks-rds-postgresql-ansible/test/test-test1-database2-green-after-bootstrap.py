from fixtures import *


def test_is_master(conn):
    res = conn.execute("SELECT * FROM pg_is_in_recovery();")
    assert res.fetchone()[0] == True
