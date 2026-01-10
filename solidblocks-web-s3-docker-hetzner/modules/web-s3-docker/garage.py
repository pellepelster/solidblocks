import sys
from os import getenv

import garage_admin_sdk
from garage_admin_sdk import NodeRoleChangeRequest
from garage_admin_sdk.api.access_key_api import *
from garage_admin_sdk.api.bucket_alias_api import *
from garage_admin_sdk.api.bucket_api import *
from garage_admin_sdk.api.cluster_layout_api import *
from garage_admin_sdk.api.node_api import *
from garage_admin_sdk.api.permission_api import *
from garage_admin_sdk.models.api_bucket_key_perm import *
from garage_admin_sdk.models.update_bucket_website_access import *

managed_access_key_name = "cloud-init"

if not getenv('ADMIN_ADDRESS') or not getenv('ADMIN_TOKEN'):
    print("'ADMIN_ADDRESS' and/or 'ADMIN_TOKEN' environment variables not set")
    exit(1)

configuration = garage_admin_sdk.Configuration(
    host=getenv('ADMIN_ADDRESS'),
    access_token=getenv('ADMIN_TOKEN')
)

api = garage_admin_sdk.ApiClient(configuration)
node_api = NodeApi(api)
bucket_api = BucketApi(api)
bucket_alias_api = BucketAliasApi(api)
access_keys_api = AccessKeyApi(api)
layout_api = ClusterLayoutApi(api)
permission_api = PermissionApi(api)

current_cluster_layout = layout_api.get_cluster_layout()
print(f"current cluster layout version is '{current_cluster_layout.version}'")

if len(current_cluster_layout.staged_role_changes) > 0:
    print(f"cluster has pending changes, aborting layout apply")
    exit(1)


node_info = node_api.get_node_info("*")

update = UpdateClusterLayoutRequest()
update.roles = [NodeRoleChangeRequest(capacity=1200000, tags=[], zone="dc1", id=info.node_id) for info in
                node_info.success.values()]

layout_api.update_cluster_layout(update)
