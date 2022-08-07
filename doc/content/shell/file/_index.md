---
title: File
weight: 30
disableToc: true
---

Helper functions for local file operations.

## Functions

### `file_extract_to_directory(compressed_file, target_dir)`

Extracts the file given by `${compressed_file}` to the directory `${target_dir}`. Appropriate decompressor is chosen depending on file extension, currently `unzip` for `*.zip` and `tar` for everything else. After uncompress a marker file is written, indicating successful decompression. If this file is present when called decompression is skipped.

```shell
source file.sh

file_extract_to_directory "file.zip" "/tmp"
```