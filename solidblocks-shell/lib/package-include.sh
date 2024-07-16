function package_update_system() {
  if which apt >/dev/null 2>&1; then
    apt_update_system
  fi
}

function package_update_repositories() {
  if which apt >/dev/null 2>&1; then
    apt_update_repositories
  fi
}

function package_ensure_package() {
  if which apt >/dev/null 2>&1; then
    apt_ensure_package $@
  fi
}

