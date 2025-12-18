import os

import pytest

from fixtures import docker_is_initialized, psql_query
from fixtures import host_is_initialized
from fixtures import psql_running
from fixtures import wait_until

test_id = os.getenv("TEST_ID")


@pytest.fixture(scope="module", autouse=True)
def wait_for_host_is_initialized(host):
    wait_until(lambda: host_is_initialized(host), 60)
    wait_until(lambda: docker_is_initialized(host, test_id), 60)
    wait_until(lambda: psql_running(host), 60)


def test_create_table(host):
    assert "rudi" in psql_query(host, "SELECT * FROM dogs;")
