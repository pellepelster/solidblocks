package de.solidblocks.cli.terraform

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import de.solidblocks.cli.utils.logInfo
import kotlin.io.path.writeText
import kotlinx.coroutines.runBlocking

enum class TYPE {
  terraform,
  tofu
}

class TerraformCommand : CliktCommand("terraform") {
  override fun help(context: Context) = "Terraform helpers"

  override fun run() {}
}

class TofuCommand : CliktCommand("tofu") {
  override fun help(context: Context) = "Tofu helpers"

  override fun run() {}
}

class BackendsCommand(val type: TYPE) : CliktCommand("backends") {
  override fun help(context: Context) = "manage $type state backends"

  override fun run() {}
}

class BackendsS3Command(val type: TYPE) : CliktCommand("s3") {

  override fun help(context: Context) =
      "Create an $type AWS S3 bucket to use for state $type storage."

  val awsSecretAccessKey by
      option("--aws-secret-access-key", envvar = "AWS_SECRET_ACCESS_KEY")
          .help { "can also be provided via variable AWS_SECRET_ACCESS_KEY" }
          .required()
  val awsAccessKeyId by
      option("--aws-access-key-id", envvar = "AWS_ACCESS_KEY_ID")
          .help { "can also be provided via variable AWS_ACCESS_KEY_ID" }
          .required()
  val awsRegion by
      option("--aws-region", envvar = "AWS_REGION")
          .help { "can also be provided via variable AWS_REGION" }
          .required()

  val key by
      option("--key").default("main").help {
        "Path to the state file inside the S3 Bucket, see also https://developer.hashicorp.com/terraform/language/backend/s3#key"
      }

  val file by
      option("--file").path().help {
        "$type config file to write, if not provided config will be printed to console only"
      }

  val bucketName by argument().help { "name for the S3 bucket" }

  override fun run() = runBlocking {
    AWS(awsAccessKeyId, awsSecretAccessKey, awsRegion).ensureBucket(bucketName)

    val config =
        """
        terraform {
          backend "s3" {
            region          = "$awsRegion"
            bucket          = "$bucketName"
            key             = "$key"
          }
        }"""
            .trimIndent()

    if (file != null) {
      logInfo("writing state configuration for created S3 bucket to '$file'")
      file!!.writeText(config)
    } else {
      logInfo("state configuration for created S3 bucket")
      println("---")
      println(config)
      println("---")
    }
  }
}
