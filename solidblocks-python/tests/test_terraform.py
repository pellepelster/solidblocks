import os

from solidblocks_do.terraform import terraform_init, terraform_apply, terraform_get_output, terraform_ensure, \
    terraform_print_output

current_dir = os.path.dirname(os.path.realpath(__file__))


def test_terraform_version_ensure():
    assert terraform_ensure()
    assert terraform_ensure("1.1.0")


def test_terraform_flow():
    assert terraform_init(f"{current_dir}/terraform")
    assert terraform_init(f"{current_dir}/terraform", ['-upgrade'])
    assert terraform_apply(f"{current_dir}/terraform", apply=True)
    assert terraform_get_output(f"{current_dir}/terraform", "foo") == "bar"
    assert terraform_print_output(f"{current_dir}/terraform")
