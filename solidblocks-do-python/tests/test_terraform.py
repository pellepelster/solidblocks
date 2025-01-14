import os

from solidblocks_do.terraform import terraform_init, terraform_apply, terraform_get_output, terraform_ensure_version


def test_terraform_version_ensure():
    terraform_ensure_version("1.1.0")


def test_terraform_init():
    terraform_init(f"{os.getcwd()}/tests/terraform")
    terraform_apply(f"{os.getcwd()}/tests/terraform")
    terraform_get_output(f"{os.getcwd()}/tests/terraform", "foo") == "bar"
