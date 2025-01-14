import json

import semver
from solidblocks_do.command import command_run, command_exec, command_exists
from solidblocks_do.log import logger


def terraform_init(path, env=None):
    command_run(['terraform', 'init', '-upgrade'], workdir=path, env=env)


def terraform_apply(path, env=None):
    command_run(['terraform', 'apply', '-auto-approve'], workdir=path, env=env)


def terraform_has_output(path, output, env=None):
    return terraform_get_output(path, output, env) is not None


def terraform_get_output(path, output, env=None):
    exitcode, stdout, stderr = command_exec(['terraform', 'output', '-raw', output], workdir=path, env=env)
    if exitcode != 0:
        return None
    return stdout.strip()


def terraform_ensure_version(min_version):
    if not command_exists('terraform'):
        logger.error(f"terraform not found on $PATH")
        return False

    exit_code, stdout, stderr = command_exec(['terraform', 'version', '-json'])

    if exit_code != 0:
        logger.error(f"failed to detect terraform version (exit code {exit_code})")
        return False

    tf_version_data = json.loads(stdout)
    tf_version = semver.Version.parse(tf_version_data['terraform_version'])
    tf_min_version = semver.Version.parse(min_version)

    if tf_version < tf_min_version:
        logger.error(f"expected at least terraform version '{min_version}' but found '{tf_version}'")
        return False

    return True
