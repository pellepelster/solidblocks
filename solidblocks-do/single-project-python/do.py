import os
import tempfile
from time import sleep

import click


@click.group()
def cli():
    pass


@click.command()
@click.option('--build-type', type=click.Choice(['debug', 'production']), help="Build type")
def build(build_type):
    """build the project"""
    click.echo(f"building the project with build type '{build_type}'")

    dir=os.path.dirname(os.path.realpath(__file__))

    with tempfile.NamedTemporaryFile(prefix=f"{dir}/.temp") as secrets_file:
        print(f"writing temporary secrets to '{secrets_file.name}'")
        with open(secrets_file.name, "w") as secrets:
            secrets.write("a confidential string")


@click.command()
@click.option('--parallel', default=2, help='Number of test to run in parallel')
def test(parallel):
    """run integration tests"""
    click.echo(f"running the integration tests ({parallel})")


cli.add_command(build)
cli.add_command(test)

if __name__ == '__main__':
    cli()
