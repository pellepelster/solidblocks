---
title: Lego
weight: 30
description: Utilities for LetsEncrypt certificate creation with lego 
---

Utilities for LetsEncrypt certificate creation with the [lego ACME client](https://github.com/go-acme/lego)

## Functions

### `lego_setup_dns(lego_path, lego_email, lego_domains, lego_dns_provider, [lego_run_hook], [lego_server])` {#lego_setup_dns}

Setup automatic LetsEncrypt certificate creation using the [lego ACME client](https://github.com/go-acme/lego). For potential LetsEncrypt account creation `lego_email` is used as contact email address and created certificates will be written to `lego_path`.
A certificate is issued for all domains provided via `lego_domains` (separated with blanks). Set `lego_dns_provider` to a lego compatible DNS provider(see https://go-acme.github.io/lego/dns/ for available options) and make sure to set the environment variables needed for the specific provider.

> This command will also configure a systemd timer that checks for certificate renewal every night. Because of the implicit configuration of the DNS resolvers via environment variables the content of those variables will be written to `/solidblocks/secrets/lego.env` 

If a fully qualified path to a `lego_run_hook` script is provided, this will be executed after certificate retrieval with the environment as described in [lego run hook](https://go-acme.github.io/lego/usage/cli/renew-a-certificate/#running-a-script-afterward)

It is possible to optionally override the LetsEncrypt server to use, using `lego_server` (for example to `https://acme-staging-v02.api.letsencrypt.org/directory` for testing purposes).

**example**
```shell
export HETZNER_API_KEY="${hetzner_dns_api_key}"
export HETZNER_HTTP_TIMEOUT="10"

lego_setup_dns "/storage/data/ssl" "pelle@pelle.io" "test.blcks.de" "hetzner"
```

