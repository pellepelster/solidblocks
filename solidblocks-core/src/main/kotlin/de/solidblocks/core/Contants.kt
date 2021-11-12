package de.solidblocks.core

object Contants {
    val CONTROLLER_POLICY_NAME = "controller"

    val BACKUP_POLICY_NAME = "backup"

    fun pkiMountName(cloudName: String) = "${cloudName}-pki"

    fun kvMountName(cloudName: String) = "${cloudName}-kv"

    fun hostSshMountName(cloudName: String) = "${cloudName}-host-ssh"

    fun userSshMountName(cloudName: String) = "${cloudName}-user-ssh"
}
