package de.solidblocks.cloudinit

import de.solidblocks.ssh.SSHKeyUtils

val RSA_PRIVATE_KEY = SSHKeyUtils.privateKeyToOpenSsh(SSHKeyUtils.RSA.generate().private)

val ED25519_PRIVATE_KEY = SSHKeyUtils.privateKeyToOpenSsh(SSHKeyUtils.ED25519.generate().private)
