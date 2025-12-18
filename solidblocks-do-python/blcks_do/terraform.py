import json

import semver

from blcks_do.command import command_run, command_exec, command_ensure_exists
from blcks_do.log import log_divider_thin, log_divider_bottom, \
    log_divider_top, log_hint, log_error


# see https://pellepelster.github.io/solidblocks/python/terraform/#terraform_init
def terraform_init(path, args=[], env=None):
    if not terraform_ensure():
        return False

    return command_run(['terraform', 'init'] + args, workdir=path, env=env)


# see https://pellepelster.github.io/solidblocks/python/terraform/#terraform_apply
def terraform_apply(path, apply=False, args=[], env=None):
    if not terraform_ensure():
        return False

    extra_args = []

    if apply:
        extra_args = ['-auto-approve']

    return command_run(['terraform', 'apply'] + extra_args + args, workdir=path, env=env)


# see https://pellepelster.github.io/solidblocks/python/terraform/#terraform_has_output
def terraform_has_output(path, output):
    if not terraform_ensure():
        return None

    return terraform_get_output(path, output) is not None


# see https://pellepelster.github.io/solidblocks/python/terraform/#terraform_get_output
def terraform_get_output(path, output):
    if not terraform_ensure():
        return None

    exitcode, stdout, stderr = command_exec(['terraform', 'output', '-raw', output], workdir=path)
    if exitcode != 0:
        return None

    return stdout.strip()


# see https://pellepelster.github.io/solidblocks/python/terraform/#terraform_print_output
def terraform_print_output(path, env=None):
    if not terraform_ensure():
        return False

    exitcode, stdout, stderr = command_exec(['terraform', 'output'], workdir=path, env=env)
    if exitcode != 0:
        return False

    log_divider_top()
    log_hint(f"terraform output for '{path}'")
    log_divider_thin()
    print(stdout.strip())
    log_divider_bottom()

    return True


# see https://pellepelster.github.io/solidblocks/python/terraform/#terraform_ensure
def terraform_ensure(min_version=None):
    if not command_ensure_exists('terraform'):
        log_error(f"terraform not found")
        return False

    if min_version is None:
        return True

    exit_code, stdout, stderr = command_exec(['terraform', 'version', '-json'])

    if exit_code != 0:
        log_error(f"failed to detect terraform version (exit code {exit_code})")
        return False

    tf_version_data = json.loads(stdout)
    tf_version = semver.Version.parse(tf_version_data['terraform_version'])
    tf_min_version = semver.Version.parse(min_version)

    if tf_version < tf_min_version:
        log_error(f"expected at least terraform version '{min_version}' but found '{tf_version}'")
        return False

    return True
