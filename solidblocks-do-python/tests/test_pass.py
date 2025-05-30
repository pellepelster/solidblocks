import os

import pytest

from solidblocks_do.secrets import pass_temp_file_env, pass_temp_file, pass_has_secret, pass_get_secret, pass_init, \
    pass_wrapper, \
    pass_get_secret_env, pass_ensure_secrets_env

current_path = os.path.dirname(os.path.realpath(__file__))


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_pass_has_secret():
    assert pass_has_secret('foo', f"{current_path}/secrets") is True
    assert pass_has_secret('invalid', f"{current_path}/secrets") is False


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_pass_get_secret():
    assert pass_get_secret('foo', f"{current_path}/secrets") == "bar"
    assert pass_get_secret('test/some/password', f"{current_path}/secrets") == "bar"
    assert pass_get_secret('test/some/secret-with-dashes', f"{current_path}/secrets") == "bar"
    assert pass_has_secret('invalid', f"{current_path}/secrets") is False


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_pass_get_secret_env():
    assert pass_get_secret_env('foo', f"{current_path}/secrets") == "foo_env"
    assert pass_get_secret_env('test/some/password', f"{current_path}/secrets") == "test_some_password_env"
    assert pass_get_secret_env('test/some/secret-with-dashes',
                               f"{current_path}/secrets") == "test_some_secret_with_dashes_env"


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_pass_ensure_secrets_env():
    assert pass_ensure_secrets_env(['foo'], f"{current_path}/secrets")
    assert pass_ensure_secrets_env(['only/from/env'], f"{current_path}/secrets")
    assert not pass_ensure_secrets_env(['foo1'], f"{current_path}/secrets")


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_pass_init():
    assert pass_get_secret('foo', f"{current_path}/secrets") == "bar"
    assert pass_init(f"{current_path}/secrets") is True
    assert pass_get_secret('foo', f"{current_path}/secrets") == "bar"


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_pass_temp_file():
    with pass_temp_file('foo', f"{current_path}/secrets") as temp_secret_file:
        assert os.path.isfile(temp_secret_file)
        assert open(temp_secret_file, 'r').read() == 'bar'


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_pass_temp_file_env():
    with pass_temp_file_env('test/some/password', f"{current_path}/secrets") as temp_secret_file:
        assert os.path.isfile(temp_secret_file)
        assert open(temp_secret_file, 'r').read() == 'test_some_password_env'


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_pass_wrapper():
    pass_wrapper(['show'], f"{current_path}/secrets")
