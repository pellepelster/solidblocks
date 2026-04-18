package de.solidblocks.cloudinit

import de.solidblocks.ssh.SSHKeyUtils

val RSA_PRIVATE_KEY = SSHKeyUtils.privateKeyToOpenSsh(SSHKeyUtils.loadKey(SSHKeyUtils.RSA.generate().privateKey).private)

val ED25519_PRIVATE_KEY = SSHKeyUtils.privateKeyToOpenSsh(SSHKeyUtils.loadKey(SSHKeyUtils.ED25519.generate().privateKey).private)
