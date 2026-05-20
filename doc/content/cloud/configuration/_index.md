+++
title = 'Configuration'
weight = 20
+++

# Substitution

The cloud configuration has capabilities to substitute configuration settings with dynamic values. Those dynamic values are configured with the syntax `${<type>:<arg1>:<arg2>:<arg n>}` and the following types are available

## Environment variables `env`

**replace with `HOME` environment variable**
```yaml
attribute1: "all my files are stored here: ${env:HOME}"
```

**replace with `VAR1` environment variable, fall back to `foo-bar` if not set**
```yaml
attribute2: "var1: ${env:VAR1:foo-bar}"
```

## Pass secrets `pass`

If a `pass` secret provider is configured, the following replacements can be used

**replace with secret at path `some/secret/path`**
```yaml
attribute3: "very secret: ${pass:some/secret/path}"
```
