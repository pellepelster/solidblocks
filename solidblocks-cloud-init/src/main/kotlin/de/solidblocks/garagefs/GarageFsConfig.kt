package de.solidblocks.garagefs

class GarageFsConfig(
    val baseStorageDir: String,
    val rpcSecret: String,
    val adminToken: String,
    val metricsToken: String,
    val s3ApiFqdn: String,
    val s3WebFqdn: String,
) {

  val metaDataDir = "$baseStorageDir/garage/meta"
  val dataDir = "$baseStorageDir/garage/data"

  fun render(): String =
      """
            metadata_dir = "$metaDataDir"
            data_dir = "$dataDir"
            db_engine = "sqlite"
            
            replication_factor = 1
            
            rpc_bind_addr = "[::]:3901"
            rpc_public_addr = "127.0.0.1:3901"
            rpc_secret = "$rpcSecret"
            
            [s3_api]
            s3_region = "garage"
            api_bind_addr = "[::]:3900"
            root_domain = "$s3ApiFqdn"
            
            [s3_web]
            bind_addr = "[::]:3902"
            root_domain = "$s3WebFqdn"
            index = "index.html"
            
            [k2v_api]
            api_bind_addr = "[::]:3904"
            
            [admin]
            api_bind_addr = "[::]:3903"
            admin_token = "$adminToken"
            metrics_token = "$metricsToken"
    """
          .trimIndent()
}
