#!/usr/bin/env bash

set -eu -o pipefail


PACKAGE_NAME="solidblocks-rds-postgresql"
USER="pellepelster"

versions=$(gh api "/users/$USER/packages/container/$PACKAGE_NAME/versions" --paginate)

for row in $(echo "${versions}" | jq -r '.[] | @base64'); do
    _jq() {
     echo ${row} | base64 --decode | jq -r ${1}
    }
    version_id="$(_jq '.id')"
    tags="$(_jq '.metadata.container.tags')"
    tags_count="$(echo $tags | jq '. | length' )"
    if [[ "${tags_count}" == "0" ]]; then
      echo "DELETING $(_jq '.name'), id: ${version_id}, tags: ${tags}"
      PAGER= gh api -X DELETE "/users/$USER/packages/container/$PACKAGE_NAME/versions/$version_id"
    else
      echo "KEEPING $(_jq '.name'), tags: ${tags}"
    fi
done
#echo "$versions" | jq -r '.[] | select(.metadata.container.tags | length > 0) | .id' | \
#while read version_id; do
#  #gh api -X DELETE "/orgs/$ORG/packages/container/$PACKAGE_NAME/versions/$version_id"
#  echo "Deleted version $version_id"
#done