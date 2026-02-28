function garage_install() {
  curl_wrapper https://garagehq.deuxfleurs.fr/_releases/v2.2.0/x86_64-unknown-linux-musl/garage -o /usr/local/bin/garage
  echo "ec761bb996e8453e86fe68ccc1cf222c73bb1ef05ae0b540bd4827e7d1931aab /usr/local/bin/garage" | sha256sum --check
  chmod +x /usr/local/bin/garage
}