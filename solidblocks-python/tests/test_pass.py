import os

import pytest

from solidblocks_do.secrets_pass import pass_temp_file, pass_has_secret, pass_get_secret, pass_init, \
    pass_wrapper, \
    pass_ensure_secrets

current_path = os.path.dirname(os.path.realpath(__file__))


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_pass_has_secret():
    assert pass_has_secret('some/password', f"{current_path}/secrets") is True
    assert pass_has_secret('override/password', f"{current_path}/secrets") is True
    assert pass_has_secret('some/secret-with-dashes', f"{current_path}/secrets") is True
    assert pass_has_secret('non/existing/path', f"{current_path}/secrets") is False
    assert pass_has_secret('non/existing/override', f"{current_path}/secrets") is True
    assert pass_has_secret('non/existing/path', env_name="CUSTOM_ENV_NAME") is True
    assert pass_has_secret('non/existing/path', f"{current_path}/secrets", env_name="CUSTOM_ENV_NAME") is True


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_pass_get_secret():
    assert pass_get_secret('some/password', f"{current_path}/secrets") == "bar"
    assert pass_get_secret('override/password', f"{current_path}/secrets") == "override_password"
    assert pass_get_secret('some/secret-with-dashes', f"{current_path}/secrets") == "bar"
    assert pass_get_secret('non/existing/path', f"{current_path}/secrets") is None
    assert pass_get_secret('non/existing/override', f"{current_path}/secrets") == "non_existing_override"
    assert pass_get_secret('non/existing/path', env_name="CUSTOM_ENV_NAME") == "custom_env_name"
    assert pass_get_secret('non/existing/path', f"{current_path}/secrets", "CUSTOM_ENV_NAME") == "custom_env_name"


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_pass_ensure_secrets():
    assert pass_ensure_secrets(['some/password'], f"{current_path}/secrets")
    assert pass_ensure_secrets(['some/password', 'some/secret-with-dashes'], f"{current_path}/secrets")
    assert pass_ensure_secrets(['some/password', 'some/secret-with-dashes'], f"{current_path}/secrets")
    assert pass_ensure_secrets(['override/password'], f"{current_path}/secrets")
    assert pass_ensure_secrets(['some/password', 'override/password'], f"{current_path}/secrets")


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_pass_init():
    assert pass_get_secret('some/password', f"{current_path}/secrets") == "bar"
    assert pass_init(f"{current_path}/secrets") is True
    assert pass_get_secret('some/password', f"{current_path}/secrets") == "bar"


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_pass_temp_file():
    with pass_temp_file('some/password', f"{current_path}/secrets") as temp_secret_file:
        assert os.path.isfile(temp_secret_file)
        assert open(temp_secret_file, 'r').read() == 'bar'

    with pass_temp_file('override/password', f"{current_path}/secrets") as temp_secret_file:
        assert os.path.isfile(temp_secret_file)
        assert open(temp_secret_file, 'r').read() == 'override_password'


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_pass_wrapper():
    pass_wrapper(['show'], f"{current_path}/secrets")
