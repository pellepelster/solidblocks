import os

from solidblocks_do.secrets import pass_has_secret


def test_pass_has_secret():
    assert pass_has_secret('foo', f"{os.getcwd()}/tests/secrets") is True
    assert pass_has_secret('invalid', f"{os.getcwd()}/tests/secrets") is False
