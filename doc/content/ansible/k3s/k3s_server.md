+++
title = "Role k3s_server"
description = "setup k3s servers"
+++

## Variables

| Name | Value | Description | Required |
| ---- | ----- | ----------- | -------- |
| cilium_hubble_export_allow_list | &lt;none&gt; | flow filter for Cilium hubble exporter, see https://docs.cilium.io/en/latest/observability/hubble/configuration/export/#configuring-hubble-exporter and https://github.com/cilium/hubble#specifying-raw-flow-filters | false  |
| cilium_ipam_mode | kubernetes | IPAM mode for cilium CNI TODO | false  |
| cilium_pod_annotations | &lt;none&gt; | &lt;none&gt; | false  |
| cilium_version | 1.17.4 | &lt;none&gt; | false  |
| k3s_api_endpoint | &lt;none&gt; | global load-balanced address for the k3s api endpoint | true  |
| k3s_data_dir | {{ storage_data_dir }}/k3s | &lt;none&gt; | false  |
| k3s_environment | &lt;none&gt; | environment name for the K3S cluster | false  |
| k3s_name | &lt;none&gt; | name for the K3S cluster | true  |
| storage_data_dir | /storage/data | &lt;none&gt; | false  |
