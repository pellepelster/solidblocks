from fixtures import *


def test_data_still_there(conn):
    res = conn.execute("SELECT * FROM table1;")
    assert res.fetchone()[0] == TEST_ID
