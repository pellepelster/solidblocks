import importlib
from datetime import datetime
from os import path

import click
from structlog import get_logger

root_infrastructure = importlib.import_module("solidblocks-root-infrastructure.do")
cloud_init = importlib.import_module("solidblocks-cloud-init.do")

logger = get_logger()


@click.group()
def cli():
    pass


@click.command("increment-version")
def cli_increment_version():
    root_dir = path.dirname(path.realpath(__file__))
    with open(f"{root_dir}/version.txt", "w") as version_file:
        version_file.write("snapshot-%s" % datetime.now().strftime("%Y%m%d%H%M%S"))


cli.add_command(root_infrastructure.cli)
cli.add_command(cloud_init.cli)
cli.add_command(cli_increment_version)

if __name__ == '__main__':
    cli()
