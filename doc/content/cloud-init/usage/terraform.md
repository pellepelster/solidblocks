---
title: Terraform
weight: 30
description: Use cloud-init function Terraform deployments
---
   
If you are in an environment where resources are provisioned using Terraform, you can use the provisioned Terraform module to generate a Solidblocks cloud-init based user data script.

**main.tf**
```terraform
{{% include "/snippets/cloud-init-kitchen-sink/main.tf" %}}
```

**variables.tf**
```terraform
{{% include "/snippets/cloud-init-kitchen-sink/variables.tf" %}}
```

**versions.tf**
```terraform
{{% include "/snippets/cloud-init-kitchen-sink/variables.tf" %}}
```

