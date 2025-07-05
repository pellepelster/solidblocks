import os

import pytest

from solidblocks_do.tofu import tofu_init, tofu_apply, tofu_get_output, tofu_ensure, \
    tofu_print_output

current_dir = os.path.dirname(os.path.realpath(__file__))


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_tofu_version_ensure():
    assert tofu_ensure()
    assert tofu_ensure("1.1.0")


@pytest.mark.skipif(
    os.getenv("CI") is not None,
    reason="ci"
)
def test_tofu_flow():
    assert tofu_init(f"{current_dir}/tofu")
    assert tofu_init(f"{current_dir}/tofu", ['-upgrade'])
    assert tofu_apply(f"{current_dir}/tofu", apply=True)
    assert tofu_get_output(f"{current_dir}/tofu", "foo") == "bar"
    assert tofu_print_output(f"{current_dir}/tofu")
