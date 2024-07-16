#!/usr/bin/env bash

set -eu -o pipefail

DIR="$(cd "$(dirname "$0")" ; pwd -P)"

if [ ! -f /.dockerenv ]; then
    echo "skipping test in docker environments"
    exit 0
fi

source "${DIR}/../../lib/test.sh"
source "${DIR}/../../lib/package.sh"
source "${DIR}/utils.sh"

package_update_repositories
package_update_system

test_assert_file_not_exists "/usr/bin/wget"
package_ensure_package "wget"
test_assert_file_exists "/usr/bin/wget"

package_ensure_package "wget"
test_assert_file_exists "/usr/bin/wget"
