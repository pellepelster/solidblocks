import os

import pytest

from fixtures import docker_is_initialized, run_docker_command, psql_command
from fixtures import host_is_initialized
from fixtures import psql_execute
from fixtures import psql_running
from fixtures import wait_until

test_id = os.getenv("TEST_ID")


@pytest.fixture(scope="module", autouse=True)
def wait_for_host_is_initialized(host):
    wait_until(lambda: host_is_initialized(host), 60)
    wait_until(lambda: docker_is_initialized(host, test_id), 60)
    wait_until(lambda: psql_running(host), 60)


def test_storge_mounts(host):
    assert host.mount_point(f"/storage/data").exists
    assert host.mount_point(f"/storage/backup").exists


def test_unattended_upgrade_enabled(host):
    assert host.file(f"/etc/apt/apt.conf.d/50unattended-upgrades").exists
    assert host.service(f"unattended-upgrades").is_enabled
    assert host.service(f"unattended-upgrades").is_running


def test_user_data(host):
    """ ensure script from user data supplied to the module was executed """
    assert host.package("telnet").is_installed


def test_environment_variables(host):
    """ extra env vars are present """
    cmd = host.run(f"docker exec rds-postgresql-{test_id}_postgresql env")
    assert "ENV1=KEY1" in cmd.stdout


def test_db_admin_password(host):
    rc, _, _ = psql_command(host, "rds", "5aee570e-b669-4df6-b05c-1b581e88325f", "database1", "SELECT 1;")
    assert rc == 0


def test_create_table(host):
    ddl = """
    CREATE TABLE dogs (
        id serial PRIMARY KEY,
        name VARCHAR (50) UNIQUE NOT NULL
    );
    """

    assert psql_execute(host, ddl) is True
    assert psql_execute(host, "INSERT into dogs (name) VALUES ('rudi');") is True
    assert run_docker_command(host, test_id, "/rds/bin/backup-full.sh") is True
