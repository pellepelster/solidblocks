import base64
import click
import ctuhl_do
import hvac
from os import path
from structlog import get_logger

logger = get_logger()


def vault_url(ctx):
    url = f"https://vault.{ctx.obj['environment']}.blcks.de:8200"
    return url


@click.group("root-infrastructure")
@click.option('--environment', required=True)
@click.pass_context
def cli(ctx, environment):
    if ctx.obj is None:
        ctx.obj = {}

    ctx.obj['environment'] = environment


@click.group()
def vault():
    pass


@click.command("storage-deploy")
@click.pass_context
def vault_deploy_storage(ctx):
    root_dir = path.dirname(path.realpath(__file__))

    hcloud_token = ctuhl_do.pass_secret("solidblocks/hetzner/cloud/api_token")

    ctuhl_do.terraform_apply({"hcloud_token": hcloud_token}, f"{root_dir}/vault/storage", ctx.obj['environment'])


@click.command("storage-destroy")
@click.pass_context
def vault_destroy_storage(ctx):
    root_dir = path.dirname(path.realpath(__file__))

    hcloud_token = ctuhl_do.pass_secret("solidblocks/hetzner/cloud/api_token")

    ctuhl_do.terraform_destroy({"hcloud_token": hcloud_token}, f"{root_dir}/vault/storage", ctx.obj['environment'])


def vault_instance_vars(environment):
    vault_1_public_key = ctuhl_do.pass_secret("solidblocks/dev/vault-1/ssh_host_ed25519_public_key")
    vault_1_key = ctuhl_do.pass_secret("solidblocks/dev/vault-1/ssh_host_ed25519_key")

    return base_instance_vars(environment) | {
        "ssh_identity_ed25519_key": str(
            base64.b64encode(vault_1_key.encode('utf-8')), 'utf-8'),
        "ssh_identity_ed25519_pub": str(
            base64.b64encode(vault_1_public_key.encode('utf-8')),
            'utf-8')}


def base_instance_vars(environment):
    hcloud_token = ctuhl_do.pass_secret("solidblocks/hetzner/cloud/api_token")
    hetznerdns_token = ctuhl_do.pass_secret("solidblocks/hetzner/dns/api_token")

    return {"hcloud_token": hcloud_token,
            "hetznerdns_token": hetznerdns_token,
            "environment": environment}


@click.command("instances-deploy")
@click.pass_context
def vault_deploy_instances(ctx):
    root_dir = path.dirname(path.realpath(__file__))
    ctuhl_do.terraform_apply(vault_instance_vars(ctx.obj['environment']), f"{root_dir}/vault/instances", ctx.obj['environment'])


@click.command("instances-destroy")
@click.pass_context
def vault_destroy_instances(ctx):
    root_dir = path.dirname(path.realpath(__file__))
    ctuhl_do.terraform_destroy(vault_instance_vars(ctx.obj['environment']), f"{root_dir}/vault/instances", ctx.obj['environment'])


def vault_vars(ctx):
    return {"vault_token": ctuhl_do.pass_secret("solidblocks/dev/vault/root_token"),
            "environment": ctx.obj['environment']}


@click.command("configure")
@click.pass_context
def vault_configure(ctx):
    client = hvac.Client(url=vault_url(ctx))

    shares = 5
    threshold = 3

    if not client.sys.is_initialized():
        logger.info(f"initializing vault '{vault_url(ctx)}'")

        result = client.sys.initialize(shares, threshold)
        ctuhl_do.pass_insert_secret("solidblocks/dev/vault/root_token", result['root_token'])
        for i, key in enumerate(result['keys']):
            ctuhl_do.pass_insert_secret(f"solidblocks/dev/vault/unseal_key_{i}", key)
    else:
        logger.info(f"vault at '{vault_url(ctx)}' already initialized")

    if client.sys.is_sealed():
        logger.info(f"unsealing vault at '{vault_url(ctx)}'")

        for i in range(0, threshold):
            client.sys.submit_unseal_key(ctuhl_do.pass_secret(f"solidblocks/dev/vault/unseal_key_{i}"))

    root_dir = path.dirname(path.realpath(__file__))

    ctuhl_do.terraform_apply(
        vault_vars(ctx) | {"github_token_ro": ctuhl_do.pass_secret("solidblocks/github/personal_access_token_ro"), },
        f"{root_dir}/vault/config", ctx.obj['environment'])


@click.command("seal")
@click.pass_context
def vault_seal(ctx):
    client = hvac.Client(url=vault_url(ctx))
    client.token = ctuhl_do.pass_secret("solidblocks/dev/vault/root_token")

    logger.info(f"sealing vault at '{vault_url(ctx)}'")
    client.sys.seal()


cli.add_command(vault)
vault.add_command(vault_destroy_storage)
vault.add_command(vault_deploy_storage)
vault.add_command(vault_deploy_instances)
vault.add_command(vault_destroy_instances)
vault.add_command(vault_configure)
vault.add_command(vault_seal)


@click.group()
def backup():
    pass


@click.command("storage-deploy")
@click.pass_context
def backup_deploy_storage(ctx):
    root_dir = path.dirname(path.realpath(__file__))

    hcloud_token = ctuhl_do.pass_secret("solidblocks/hetzner/cloud/api_token")
    ctuhl_do.terraform_apply({"hcloud_token": hcloud_token}, f"{root_dir}/backup/storage", ctx.obj['environment'])


@click.command("storage-destroy")
@click.pass_context
def backup_destroy_storage(ctx):
    root_dir = path.dirname(path.realpath(__file__))

    hcloud_token = ctuhl_do.pass_secret("solidblocks/hetzner/cloud/api_token")
    ctuhl_do.terraform_destroy({"hcloud_token": hcloud_token}, f"{root_dir}/backup/storage", ctx.obj['environment'])


@click.command("instances-deploy")
@click.option('--solidblocks-version', required=True)
@click.pass_context
def backup_deploy_instances(ctx, solidblocks_version):
    root_dir = path.dirname(path.realpath(__file__))
    ctuhl_do.terraform_apply(
        base_instance_vars(ctx.obj['environment']) | vault_vars(ctx) | {"solidblocks_version": solidblocks_version},
        f"{root_dir}/backup/instances", ctx.obj['environment'])


@click.command("instances-destroy")
@click.pass_context
def backup_destroy_instances(ctx):
    root_dir = path.dirname(path.realpath(__file__))
    ctuhl_do.terraform_destroy(base_instance_vars(ctx.obj['environment']) | vault_vars(ctx),
                               f"{root_dir}/backup/instances",
                               ctx.obj['environment'])


cli.add_command(backup)
backup.add_command(backup_destroy_storage)
backup.add_command(backup_deploy_storage)
backup.add_command(backup_deploy_instances)
backup.add_command(backup_destroy_instances)

cli.add_command(vault)
