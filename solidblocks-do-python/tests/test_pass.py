import os

from solidblocks_do.secrets import pass_has_secret, pass_get_secret, pass_init, pass_wrapper, pass_get_secret_env

current_path = os.path.dirname(os.path.realpath(__file__))


def test_pass_has_secret():
    assert pass_has_secret('foo', f"{current_path}/secrets") is True
    assert pass_has_secret('invalid', f"{current_path}/secrets") is False


def test_pass_get_secret():
    assert pass_get_secret('foo', f"{current_path}/secrets") == "bar"
    assert pass_get_secret('test/some/password', f"{current_path}/secrets") == "foo"
    assert pass_get_secret('test/some/secret-with-dashes', f"{current_path}/secrets") == "bar"
    assert pass_has_secret('invalid', f"{current_path}/secrets") is False


def test_pass_get_secret_env():
    assert pass_get_secret_env('foo', f"{current_path}/secrets") == "bar"
    assert pass_get_secret_env('test/some/password', f"{current_path}/secrets") == "override_from_env"
    assert pass_get_secret_env('test/some/secret-with-dashes', f"{current_path}/secrets") == "override_from_env1"


def test_pass_init():
    assert pass_get_secret('foo', f"{current_path}/secrets") == "bar"
    assert pass_init(f"{current_path}/secrets") is True
    assert pass_get_secret('foo', f"{current_path}/secrets") == "bar"


def test_pass_wrapper():
    pass_wrapper(['show'], f"{current_path}/secrets")
