export POSTGRES_KEY_CHECKSUM="0144068502a1eddd2a0280ede10ef607d1ec592ce819940991203941564e8e76"

function postgres_add_repository() {
  install -d /usr/share/postgresql-common/pgdg
  curl_wrapper https://www.postgresql.org/media/keys/ACCC4CF8.asc -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc
  echo "${POSTGRES_KEY_CHECKSUM}  /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc" | sha256sum -c
  . /etc/os-release
  echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt $VERSION_CODENAME-pgdg main" > /etc/apt/sources.list.d/pgdg.list
  apt-get update
}

function postgres_install() {
  local version=${1:-17}
  apt-get install -y postgresql-${version} ripgrep
  export PATH=/rds/bin:/usr/lib/postgresql/${version}/bin:$PATH
}

function postgres_current_major_version() {
  postgres --version | rg ' ([0-9]{1,})\.[0-9]{1,} ' -or '$1'
}
