function postgres_add_repository() {
  install -d /usr/share/postgresql-common/pgdg
  curl_wrapper https://www.postgresql.org/media/keys/ACCC4CF8.asc -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc
  echo "${POSTGRES_KEY_CHECKSUM}  /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc" | sha256sum -c
  . /etc/os-release
  echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt $VERSION_CODENAME-pgdg main" > /etc/apt/sources.list.d/pgdg.list
}
