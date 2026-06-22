package de.solidblocks.cloud.provisioner.secret

import de.solidblocks.cloud.api.ResourceProvisioner

interface GenericSecretProvisioner :
    ResourceProvisioner<GenericSecret, GenericSecretRuntime, GenericSecretLookup>
