#!/usr/bin/env bash

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

set -eu -o pipefail

source "${DIR}/../../lib/aws.sh"
source "${DIR}/../../lib/test.sh"

test_assert_matches "non owned bucket"      "error(.*)"         "$(aws_bucket_exists "yolo")"
test_assert_equals  "non existing bucket"   "bucket_not_found"  "$(aws_bucket_exists "$(uuidgen)")"
test_assert_matches "bucket name too short" "error(.*)"         "$(aws_bucket_exists "xx")"

TEST_BUCKET_NAME="test-$(uuidgen)"

test_assert_equals "non existing bucket"    "bucket_not_found"        "$(aws_bucket_exists "${TEST_BUCKET_NAME}")"
aws_bucket_ensure "${TEST_BUCKET_NAME}"
test_assert_equals "existing bucket"        "bucket_exists"           "$(aws_bucket_exists "${TEST_BUCKET_NAME}")"

# ensure idempotency
aws_bucket_ensure "${TEST_BUCKET_NAME}"

aws_bucket_delete "${TEST_BUCKET_NAME}"
test_assert_equals "non existing bucket"    "bucket_not_found"        "$(aws_bucket_exists "${TEST_BUCKET_NAME}")"

TEST_TABLE_NAME="test-$(uuidgen)"

# ensure table does not exist yet
test_assert_equals "table exists"  "table_not_found"     "$(aws_dynamodb_table_exists "${TEST_TABLE_NAME}")"

# delete on a non resisting tabe should have no effect
aws_dynamodb_delete "${TEST_TABLE_NAME}"

# create table
aws_dynamodb_ensure "${TEST_TABLE_NAME}" "AttributeName=LockID,AttributeType=S" "AttributeName=LockID,KeyType=HASH"
test_assert_equals "table exists"  "table_exists"     "$(aws_dynamodb_table_exists "${TEST_TABLE_NAME}")"

# ensure idempotency for create table
aws_dynamodb_ensure "${TEST_TABLE_NAME}" "AttributeName=LockID,AttributeType=S" "AttributeName=LockID,KeyType=HASH"

# delete table again
aws_dynamodb_delete "${TEST_TABLE_NAME}"
test_assert_equals "table exists"  "table_not_found"     "$(aws_dynamodb_table_exists "${TEST_TABLE_NAME}")"
