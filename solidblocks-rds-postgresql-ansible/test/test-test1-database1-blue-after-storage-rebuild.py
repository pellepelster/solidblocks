from pprint import pprint

from fixtures import conn, ip_addr, TEST_ID


def test_data_still_there(conn):
    res = conn.execute("SELECT * FROM table1;")
    assert res.fetchone()[0] == TEST_ID
