import os
import subprocess
from os import path

import click
import ctuhl_do
from structlog import get_logger

logger = get_logger()


@click.group("cloud-init")
def cli():
    pass


@click.command()
def deploy():
    root_dir = path.dirname(path.realpath(__file__))

    api_token = ctuhl_do.pass_secret("solidblocks/github/personal_access_token_rw")

    env = os.environ.copy()
    env['USERNAME'] = "pellepelster"
    env['TOKEN'] = api_token

    subprocess.run([f"{root_dir}/../gradlew", "solidblocks-cloud-init:publish"], env=env)


cli.add_command(deploy)
