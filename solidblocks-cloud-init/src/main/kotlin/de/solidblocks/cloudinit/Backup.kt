package de.solidblocks.cloudinit

data class BackupConfiguration(val password: String, val target: BackupTarget)

sealed class BackupTarget

class S3BackupTarget(val bucket: String, val accessKey: String, val secretKey: String) : BackupTarget()

class LocalBackupTarget(val backupDevice: String) : BackupTarget()
