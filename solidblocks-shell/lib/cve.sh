# https://github.com/v12-security/pocs/tree/main/fragnesia
function cve_fragnesia {
  if lsmod | grep esp4; then
    rmmod esp4
  fi

  if lsmod | grep esp6; then
    rmmod esp6
  fi

  if lsmod | grep rxrpc; then
    rmmod rxrpc
  fi
  printf 'install esp4 /bin/false\ninstall esp6 /bin/false\ninstall rxrpc /bin/false\n' > /etc/modprobe.d/dirtyfrag.conf
}

