import os

from solidblocks_do.command import command_run_interactive, command_exec
from solidblocks_do.log import logger


def pass_env(secret_store):
    if secret_store is None:
        secret_store = f"{os.getcwd()}/secrets"

    if not os.path.isdir(secret_store):
        logger.error(f"pass secret store not found at '{secret_store}")
        return None

    return {"PASSWORD_STORE_DIR": secret_store}


def pass_insert_secret(path, secret_store=None):
    env = pass_env(secret_store)
    if env is None:
        return False
    command_run_interactive(['pass', 'insert', path], )
    return True


def pass_get_secret(path, secret_store=None):
    exitcode, stdout, _ = command_exec(['pass', path], pass_env(secret_store))
    if exitcode == 0:
        return stdout.strip()
    else:
        return None


def pass_has_secret(path, secret_store=None):
    return pass_get_secret(path, secret_store) is not None
