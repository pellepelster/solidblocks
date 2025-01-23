import os

from solidblocks_do.terraform import terraform_init, terraform_apply, terraform_get_output, terraform_ensure, \
    terraform_pretty_print_output


def test_terraform_version_ensure():
    assert terraform_ensure()
    assert terraform_ensure("1.1.0")


def test_terraform_init():
    assert terraform_init(f"{os.getcwd()}/tests/terraform")
    assert terraform_apply(f"{os.getcwd()}/tests/terraform")
    assert terraform_get_output(f"{os.getcwd()}/tests/terraform", "foo") == "bar"
    assert terraform_pretty_print_output(f"{os.getcwd()}/tests/terraform")
