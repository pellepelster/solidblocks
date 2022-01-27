# Solidblocks 3

## TODOs
* pellepelster github is currently SPOF, better an organization?
* token lifetimes and max ttl?
* ensure uniqueness of hetzner credentials over all clouds
* csrf protection
* 
## Architecture

### Filesystem structure

* SOLIDBLOCKS_DIR
  * /solidblocks/instance/environment
    * SOLIDBLOCKS_VERSION

* all TLS stuff has to work without interaction from the outside (certificate creation/renewal)