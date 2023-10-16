import base64
import time
from pprint import pprint


def wait_until(predicate, timeout, period=2, *args, **kwargs):
    end = time.time() + timeout
    while time.time() < end:
        if predicate(*args, **kwargs):
            return True
        time.sleep(period)
    return False


def host_is_initialized(host):
    return host.file("/run/cloud-init/result.json").exists


def docker_is_initialized(host, test_id):
    docker = host.docker(f"rds-postgresql-{test_id}_postgresql")
    return docker.is_running


def psql_running(host):
    rc, stdout, _ = psql_command(host, "user1", "password1", "database1", "SELECT 1;")
    return stdout == "1"


def psql_execute(host, sql):
    rc, _, _ = psql_command(host, "user1", "password1", "database1", sql)
    return rc == 0


def psql_query(host, sql):
    rc, stdout, _ = psql_command(host, "user1", "password1", "database1", sql)
    return stdout


def psql_command(host, user, password, database, sql):
    sql_base64 = base64.b64encode(sql.encode("ascii")).decode("ascii")
    return run_command(host,
                       f"echo {sql_base64} | base64 -d | PGPASSWORD={password} psql --host localhost --user {user} --tuples-only {database}")


def run_docker_command(host, test_id, command):
    cmd = host.run(f"docker exec rds-postgresql-{test_id}_postgresql {command}")
    # pprint(cmd.stderr.strip())
    return cmd.rc == 0


def run_command(host, command):
    cmd = host.run(f"{command}")

    if cmd.rc > 0:
        pprint(cmd.stderr)

    return cmd.rc, cmd.stdout.strip(), cmd.stderr
