import os

from solidblocks_do.command import command_run_interactive, command_exec, command_exists
from solidblocks_do.log import logger


def pass_env(secret_store):
    if secret_store is None:
        secret_store = f"{os.getcwd()}/secrets"

    if not os.path.isdir(secret_store):
        logger.error(f"pass secret store not found at '{secret_store}'")
        return None

    if not os.path.isfile(f"{secret_store}/.gpg-id"):
        logger.error(f"directory '{secret_store}' does not look like a secret store, '.gpg-id' is missing")
        return None

    return {"PASSWORD_STORE_DIR": secret_store}


def pass_wrapper(command=None, secret_store=None):
    env = pass_env(secret_store)
    if env is None:
        return None

    if not command_exists('pass'):
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

    if not command_exists('pass'):
        return None

    command_run_interactive(['pass', 'insert'] + (['--multiline'] if multiline else []) + [path],
                            dict(os.environ) | env)
    return True


def pass_get_secret(path, secret_store=None):
    return pass_wrapper([path], secret_store)


def pass_get_secret_env(path, secret_store=None):
    env_name = path.replace("/", "_").replace("-", "_").upper()
    if os.getenv(env_name):
        logger.info(f"found environment variable '{env_name}' for secret with path '{path}'")
        return os.getenv(env_name)

    return pass_wrapper([path], secret_store)


def pass_has_secret(path, secret_store=None):
    return pass_get_secret(path, secret_store) is not None


def pass_init(secret_store=None):
    env = pass_env(secret_store)
    if env is None:
        return False

    secret_store = env['PASSWORD_STORE_DIR']
    gpg_ids = open(f"{secret_store}/.gpg-id", 'r').read().splitlines()

    exitcode, _, _ = command_exec(['pass', 'init', ' '.join(gpg_ids)], env)
    return exitcode == 0
