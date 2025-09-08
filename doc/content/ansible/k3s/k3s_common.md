+++
title = "Role k3s_common"
description = "deploy common components for K3S cluster servers and agents"
+++

deploy common components for K3S cluster servers and agents

## Variables

| Name | Value | Description | Required |
| ---- | ----- | ----------- | -------- |
| cluster_cidr | {{ (server_config_yaml | from_yaml)['cluster-cidr'] | default('10.42.0.0/16') }} | &lt;none&gt; | false  |
| k3s_version | v1.32.4+k3s1 | &lt;none&gt; | false  |
| service_cidr | {{ (server_config_yaml | from_yaml)['service-cidr'] | default('10.43.0.0/16') }} | &lt;none&gt; | false  |