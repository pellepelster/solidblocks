def test_storage_mounts(host):
    lines = host.check_output(
        "find /storage/**/archive/* -name '*.gz' -exec od -A none --format x1 --read-bytes 8 {} \;").splitlines()

    assert len(lines) >= 4
    assert all(line.strip() == "53 61 6c 74 65 64 5f 5f" for line in lines)