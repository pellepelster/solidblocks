import sys
from os import getenv

import garage_admin_sdk
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


class S3Bucket:
    name: str
    web_access_public_enable: str
    web_access_domains: list[str]
    owner_key_id: str
    owner_secret_key: str
    ro_key_id: str = None
    ro_secret_key: str = None
    rw_key_id: str = None
    rw_secret_key: str = None

    def __init__(self, name: str,
                 web_access_domains: list[str],
                 web_access_public_enable:
                 str, owner_key_id: str, owner_secret_key: str,
                 ro_key_id: str = None, ro_secret_key: str = None,
                 rw_key_id: str = None, rw_secret_key: str = None):
        self.name = name
        self.web_access_public_enable = web_access_public_enable
        self.web_access_domains = web_access_domains
        self.owner_key_id = owner_key_id
        self.owner_secret_key = owner_secret_key
        self.ro_key_id = ro_key_id
        self.ro_secret_key = ro_secret_key
        self.rw_key_id = rw_key_id
        self.rw_secret_key = rw_secret_key


s3_buckets_file = sys.argv[1]

print(f"loading file '{s3_buckets_file}'")
with open(s3_buckets_file, 'r') as file:
    raw_list = json.loads(file.read())

s3_buckets = [S3Bucket(**item) for item in raw_list]


def s3_buckets_has_access_key(key_id):
    for s3_bucket in s3_buckets:
        if s3_bucket.owner_key_id == key_id or s3_bucket.ro_key_id == key_id:
            return True
    return False


def has_key(managed_access_keys: list[ListKeysResponseItem], key_id):
    for key in managed_access_keys:
        if key.id == key_id:
            return True
    return False


def s3_buckets_apply():
    print(f"fetching node info")
    node_info = node_api.get_node_info("*")

    for node in node_info.success.values():
        print(f"node '{node.node_id}' is running version: '{node.garage_version}'")

    all_access_keys = access_keys_api.list_keys()

    print(f"fetching managed access keys with name '{managed_access_key_name}'")
    managed_access_keys = [x for x in all_access_keys if x.name == "cloud-init"]
    for key in managed_access_keys:
        print(f"found managed access key '{key.id}'")

    managed_access_keys_to_disable = [access_key for access_key in managed_access_keys if
                                      not s3_buckets_has_access_key(access_key.id)]

    for key in managed_access_keys_to_disable:
        print(f"disabling unused access key '{key.id}'")

    for s3_bucket in s3_buckets:

        if not bucket_exists(s3_bucket.name):
            print(f"bucket '{s3_bucket.name}' not found, creating...")
            bucket_api.create_bucket(CreateBucketRequest(global_alias=s3_bucket.name))
            print(f"created bucket '{s3_bucket.name}'")

        bucket_info = bucket_api.get_bucket_info(search=s3_bucket.name)

        if bucket_info.website_access != s3_bucket.web_access_public_enable:
            print(f"setting bucket '{s3_bucket.name}' website access to {s3_bucket.web_access_public_enable}")
            bucket_api.update_bucket(bucket_info.id, UpdateBucketRequestBody(
                website_access=UpdateBucketWebsiteAccess(enabled=s3_bucket.web_access_public_enable,
                                                         index_document="index.html")))

        for web_access_domain in s3_bucket.web_access_domains:
            if web_access_domain in bucket_info.global_aliases:
                print(f"web domain '{web_access_domain}' is already an alias for bucket '{s3_bucket.name}'")
            else:
                print(f"adding web domain '{web_access_domain}' as alias for bucket '{s3_bucket.name}'")
                bucket_alias_api.add_bucket_alias(
                    AddBucketAliasRequest(bucket_id=bucket_info.id, local_alias="", access_key_id="",
                                          global_alias=web_access_domain))

        for global_alias in bucket_info.global_aliases:
            if global_alias != s3_bucket.name and global_alias not in s3_bucket.web_access_domains:
                print(f"removing global alias '{global_alias}' from bucket '{s3_bucket.name}'")
                bucket_alias_api.remove_bucket_alias(
                    RemoveBucketAliasRequest(bucket_id=bucket_info.id, global_alias=global_alias, local_alias="",
                                             access_key_id=""))

        if s3_bucket.owner_key_id and s3_bucket.owner_secret_key:
            access_key_ensure(managed_access_keys, s3_bucket.owner_key_id, s3_bucket.owner_secret_key)
            bucket_ensure_permissions(bucket_info, s3_bucket.owner_key_id, True, True, True)

        if s3_bucket.rw_key_id and s3_bucket.rw_secret_key:
            access_key_ensure(managed_access_keys, s3_bucket.rw_key_id, s3_bucket.rw_secret_key)
            bucket_ensure_permissions(bucket_info, s3_bucket.rw_key_id, False, True, True)

        if s3_bucket.ro_key_id and s3_bucket.ro_secret_key:
            access_key_ensure(managed_access_keys, s3_bucket.ro_key_id, s3_bucket.ro_secret_key)
            bucket_ensure_permissions(bucket_info, s3_bucket.ro_key_id, False, False, True)

        for key_to_disable in managed_access_keys_to_disable:
            bucket_ensure_no_permissions(bucket_info, key_to_disable.id)


def access_key_ensure(managed_access_keys: list[ListKeysResponseItem], key_id: str, key_secret: str):
    if has_key(managed_access_keys, key_id):
        print(f"access key '{key_id}' already exists")
    else:
        print(f"importing access key '{key_id}'")
        access_keys_api.import_key(ImportKeyRequest(
            name=managed_access_key_name,
            access_key_id=key_id,
            secret_access_key=key_secret)
        )


def bucket_ensure_permissions(bucket_info: GetBucketInfoResponse, key_id: str, owner: bool, write: bool, read: bool):
    if bucket_has_key_with_permissions(bucket_info, key_id, owner, write, read):
        print(
            f"key '{key_id}' has already permissions owner: {owner}, write: {write}, read: {read} for bucket '{bucket_info.id}' ")
    else:
        print(
            f"setting permissions owner: {owner}, write: {write}, read: {read} for key '{key_id}' on bucket '{bucket_info.id}' ")
        permission_api.allow_bucket_key(
            BucketKeyPermChangeRequest(access_key_id=key_id, bucket_id=bucket_info.id,
                                       permissions=ApiBucketKeyPerm(owner=owner, read=read, write=write)))


def bucket_has_key(bucket_info: GetBucketInfoResponse, key_id: str):
    for key in bucket_info.keys:
        if key.access_key_id == key_id:
            return True

    return False


def bucket_ensure_no_permissions(bucket_info: GetBucketInfoResponse, key_id: str):
    if bucket_has_key(bucket_info, key_id):
        print(
            f"removing permissions for key '{key_id}' on bucket '{bucket_info.id}' ")
        permission_api.deny_bucket_key(
            BucketKeyPermChangeRequest(access_key_id=key_id, bucket_id=bucket_info.id,
                                       permissions=ApiBucketKeyPerm(owner=True, read=True, write=True)))


def bucket_has_key_with_permissions(bucket_info: GetBucketInfoResponse, key_id: str, owner: bool, write: bool,
                                    read: bool):
    for key in bucket_info.keys:
        if key.access_key_id == key_id and key.permissions.owner == owner and key.permissions.write == write and key.permissions.read == read:
            return True

    return False


def bucket_exists(bucket_name: str):
    try:
        bucket_api.get_bucket_info(search=bucket_name)
        return True
    except garage_admin_sdk.exceptions.NotFoundException:
        return False


s3_buckets_apply()
