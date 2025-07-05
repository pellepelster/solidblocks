import os
import tempfile

from solidblocks_do import log_error, log_hint
from solidblocks_do.command import command_run_interactive, command_exec, command_ensure_exists


def pass_env(secret_store):
    if secret_store is None:
        secret_store = f"{os.getcwd()}/secrets"

    if not os.path.isdir(secret_store):
        log_error(f"pass secret store not found at '{secret_store}'")
        return None

    if not os.path.isfile(f"{secret_store}/.gpg-id"):
        log_error(f"directory '{secret_store}' does not look like a secret store, '.gpg-id' is missing")
        return None

    return {"PASSWORD_STORE_DIR": secret_store}


def pass_wrapper(command=None, secret_store=None):
    env = pass_env(secret_store)
    if env is None:
        return None

    if not command_ensure_exists('pass'):
        return None

    exitcode, stdout, _ = command_exec(['pass'] + command, env)
    if exitcode == 0:
        return stdout.strip()
    else:
        return None


def pass_insert_secret(path, secret_store=None, multiline=False):
    env = pass_env(secret_store)

    if env is None:
        return False

    if not command_ensure_exists('pass'):
        return None

    command_run_interactive(['pass', 'insert'] + (['--multiline'] if multiline else []) + [path],
                            dict(os.environ) | env)
    return True


# see https://pellepelster.github.io/solidblocks/python/pass/#pass_ensure_secrets
def pass_ensure_secrets(paths, secret_store=None, env_name=None):
    missing_secret = True
    for path in paths:
        env_name = pass_env_name(path, env_name)
        env_secret = os.getenv(env_name)
        secret = pass_get_secret(path, secret_store)
        if secret is None and env_secret is None:
            log_error(
                f"secret '{path}' could not be read from pass, and is not set via environment variable '{env_name}'")
            missing_secret = False
        else:
            if secret:
                log_hint(
                    f"found secret '{path}' in pass secret store")
            if env_secret:
                log_hint(
                    f"found secret '{path}' via environment variable '{env_name}'")

    return missing_secret


def pass_env_name(path, env_name):
    if env_name is not None:
        return env_name
    else:
        return path.replace("/", "_").replace("-", "_").upper()


# see https://pellepelster.github.io/solidblocks/python/pass/#pass_get_secret
def pass_get_secret(path, secret_store=None, env_name=None):
    env_name = pass_env_name(path, env_name)

    if os.getenv(env_name):
        log_hint(f"found environment variable '{env_name}' for secret with path '{path}'")
        return os.getenv(env_name)

    return pass_wrapper([path], secret_store)


# see https://pellepelster.github.io/solidblocks/python/pass/#pass_has_secret
def pass_has_secret(path, secret_store=None, env_name=None):
    return pass_get_secret(path, secret_store, env_name) is not None


class PassTempFileContext:

    def __init__(self, path, secret_store=None):
        self.path = path
        self.secret_store = secret_store

    def __enter__(self):
        self.temp_file = tempfile.NamedTemporaryFile()

        secret = pass_get_secret(self.path, self.secret_store)

        if secret is None:
            log_error(f"no secret found for key '{self.path}' from_env: {self.from_env}")
            return None

        self.temp_file.write(str.encode(secret))
        self.temp_file.flush()
        log_hint(f"wrote pass secret '{self.path}' to '{self.temp_file.name}'")
        return self.temp_file.name

    def __exit__(self, exc_type, exc_value, exc_tb):
        log_hint(f"cleaning up pass secret file '{self.temp_file.name}'")
        self.temp_file.close()


# see https://pellepelster.github.io/solidblocks/python/pass/#pass_temp_file
def pass_temp_file(path, secret_store=None):
    return PassTempFileContext(path, secret_store)


def pass_init(secret_store=None):
    env = pass_env(secret_store)
    if env is None:
        return False

    secret_store = env['PASSWORD_STORE_DIR']
    gpg_ids = open(f"{secret_store}/.gpg-id", 'r').read().splitlines()

    exitcode, _, _ = command_exec(['pass', 'init', ' '.join(gpg_ids)], env)
    return exitcode == 0
