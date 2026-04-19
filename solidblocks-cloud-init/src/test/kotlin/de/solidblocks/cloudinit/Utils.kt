package de.solidblocks.cloudinit

import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.ssh.toPem

val RSA_KEY = SSHKeyUtils.RSA.generate()
val RSA_KEY_PEM = RSA_KEY.toPem()

val ED25519_KEY = SSHKeyUtils.ED25519.generate()
val ED25519_PRIVATE_KEY = SSHKeyUtils.privateKeyToOpenSsh(ED25519_KEY.private)
