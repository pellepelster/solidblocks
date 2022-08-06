![ctuhl](https://github.com/pellepelster/ctuhl/workflows/ctuhl/badge.svg)


# Commons Tools Utilities Helper and Libraries (cthul)

Repository aggregating all the stuff that accumulates over time and may be helpful to others but is not worth an own repository.

## Shell

### Commands

### k8s-port-forward

For a detailed description look [here]([k8s-port-forward](http://localhost:1313/posts//posts/ctuhl-k8s-port-forward/))

```
./k8s-port-forward                                          
starts a socat kubernetes service and forwards a randomly
chosen local tcp port via socat to tcp:<target host>:<target port>

usage: ./k8s-port-forward -h <target host> -p <target port>
```

### Library Functions

The shell utilities are designed to be used by sourcing the appropriate files, for example

```
#!/usr/bin/env bash

source "${PATH_TO_CTUHL}/download.sh"

ctuhl_ensure_terraform "/bin"
```

### `ctuhl_download_and_verify_checksum ${url} ${target_file} ${checksum}`

Downloads the file given by `${url}` to `${target_file}` and verifies if the downloaded file has the checksum `${checksum}`. If a file is already present at `${target}` download is skipped.

*example*
```
ctuhl_download_and_verify_checksum "https://releases.hashicorp.com/nomad/0.12.5/nomad_0.12.5_linux_amd64.zip" "/tmp/file.zip" "dece264c86a5898a18d62d6ecca469fee71329e444b284416c57bd1e3d76f253" 
```

### `ctuhl_extract_file_to_directory ${compressed_file} ${target_dir}`

Extracts the file given by `${compressed_file}` to the directory `${target_dir}`. Appropiate decompressor is chosen depending on file extension, currently `unzip` for `*.zip` and `tar` for everything else. After uncompress a marker file is written, indicating successful decompression. If this file is present when called decompression is skipped.

*example*
```
ctuhl_extract_file_to_directory "/downloads/file.zip" "/tmp/data"
```

### `ctuhl_ensure_hashicorp ${product} ${version} ${checksum} ${bin_dir}`

Generic wrapper for downloading HasiCorp tools built around the convention that product distributions are available at https://releases.hashicorp.com/`${product}`/`${version}`/`${product}`_`${product}`_linux_amd64.zip and the downloaded zip contains an executable named `${product}` which will be written to `${bin_dir}`.

*example*
```
ctuhl_ensure_hashicorp "terraform" "0.13.4" "a92df4a151d390144040de5d18351301e597d3fae3679a814ea57554f6aa9b24" "/bin" 

/bin/terraform [...]
```

### `ctuhl_ensure_terraform ${bin_dir} ${version} ${checksum}`

Downloads terraform `${version}` (download will be checked against `${checksum}`) to `${bin_dir}`. If `${version}` and `${checksum}` is ommited a recent-ish version will be downloaded.

*example*
```
# download the default version
ctuhl_ensure_terraform "/bin"

/bin/terraform [...]

# download a specific version
ctuhl_ensure_terraform "/tmp" "0.12.23" "78fd53c0fffd657ee0ab5decac604b0dea2e6c0d4199a9f27db53f081d831a45"

/bin/terraform [...]
```

### `ctuhl_ensure_consul ${bin_dir} ${version} ${checksum}`

Downloads Consul `${version}` (download will be checked against `${checksum}`) to `${bin_dir}`. If `${version}` and `${checksum}` is ommited a recent-ish version will be downloaded.

*example*
```
# download the default version
ctuhl_ensure_consul "/bin"

/bin/consul [...]
```