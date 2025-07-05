import os

import pytest

from solidblocks_do.command import command_ensure_exists, command_run, command_run_interactive, command_exec, \
    command_exec_json

current_path = os.path.dirname(os.path.realpath(__file__))


# see https://pellepelster.github.io/solidblocks/python/command/#command_ensure_exists
def test_command_ensure_exists():
    assert command_ensure_exists('ls') is True
    assert command_ensure_exists('invalid') is False


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_command_run_interactive():
    assert command_run_interactive(['ls']) is True


def test_command_run():
    assert command_run(['ls']) is True
    assert command_run(['ls'], shell=False) is True

    assert command_run('ls', shell=True) is True
    assert command_run('ls') is True

    assert command_run(['ls', '-lsa']) is True

    assert command_run(['invalid']) is False
    assert command_run('invalid') is False
    assert command_run(['invalid'], shell=False) is False

    if command_run(['env', '--debug'], env={"SOME_ENV": "foo-bar"}, workdir="/tmp"):
        pass


def test_command_exec():
    exitcode, stdout, stderr = command_exec(['whoami'])
    assert exitcode == 0
    assert stdout == f"{os.getenv('USER')}\n"


def test_command_exec_json():
    result = command_exec_json([f"{current_path}/test_json_command.sh"])
    assert result['attr1'] == "value1"


def test_command_exec_json_error():
    result = command_exec_json([f"{current_path}/test_json_command_error.sh"])
    assert result['attr1'] == "value1"


def test_command_exec_json_invalid():
    result = command_exec_json([f"{current_path}/test_json_command_invalid.sh"])
    assert result == None


def test_command_exec_json_empty():
    result = command_exec_json([f"{current_path}/test_json_command_empty.sh"])
    assert result == None
