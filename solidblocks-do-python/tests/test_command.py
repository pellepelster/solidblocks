import os

import pytest

from solidblocks_do.command import command_exists, command_run, command_run_interactive, command_exec


def test_command_exists():
    assert command_exists('ls') is True
    assert command_exists('invalid') is False


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_command_run_interactive():
    assert command_run_interactive(['ls']) is True


def test_command_run():
    assert command_run(['ls']) is True
    assert command_run(['invalid']) is False


def test_command_exec():
    exitcode, stdout, stderr = command_exec(['whoami'])
    assert exitcode == 0
    assert stdout == f"{os.getenv('USER')}\n"
