package de.solidblocks.terraform

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.coroutines.runBlocking

class CliCommand : CliktCommand() {
  override fun run() {}
}

class BackendCommand : CliktCommand() {
  override fun run() {}
}

class S3Command : CliktCommand(help = "create AWS S3 backend") {
  val awsSecretAccessKey by
      option("--aws-secret-access-key", envvar = "AWS_SECRET_ACCESS_KEY").required()
  val awsAccessKeyId by option("--aws-access-key-id", envvar = "AWS_ACCESS_KEY_ID").required()
  val awsRegion by option("--aws-region", envvar = "AWS_REGION").required()
  val bucketName by argument()

  override fun run() = runBlocking {
    AWS(awsAccessKeyId, awsSecretAccessKey, awsRegion).ensureBucket(bucketName)
  }
}

fun main(args: Array<String>) =
    CliCommand().subcommands(BackendCommand().subcommands(S3Command())).main(args)
