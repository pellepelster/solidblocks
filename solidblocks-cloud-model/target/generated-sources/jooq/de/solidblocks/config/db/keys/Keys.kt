/*
 * This file is generated by jOOQ.
 */
package de.solidblocks.config.db.keys


import de.solidblocks.config.db.tables.Clouds
import de.solidblocks.config.db.tables.ConfigurationValues
import de.solidblocks.config.db.tables.Environments
import de.solidblocks.config.db.tables.Services
import de.solidblocks.config.db.tables.Tenants
import de.solidblocks.config.db.tables.Users
import de.solidblocks.config.db.tables.records.CloudsRecord
import de.solidblocks.config.db.tables.records.ConfigurationValuesRecord
import de.solidblocks.config.db.tables.records.EnvironmentsRecord
import de.solidblocks.config.db.tables.records.ServicesRecord
import de.solidblocks.config.db.tables.records.TenantsRecord
import de.solidblocks.config.db.tables.records.UsersRecord

import org.jooq.ForeignKey
import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.Internal



// -------------------------------------------------------------------------
// UNIQUE and PRIMARY KEY definitions
// -------------------------------------------------------------------------

val PK_CLOUDS: UniqueKey<CloudsRecord> = Internal.createUniqueKey(Clouds.CLOUDS, DSL.name("PK_CLOUDS"), arrayOf(Clouds.CLOUDS.ID), true)
val PK_CONFIGURATION_VALUES: UniqueKey<ConfigurationValuesRecord> = Internal.createUniqueKey(ConfigurationValues.CONFIGURATION_VALUES, DSL.name("PK_CONFIGURATION_VALUES"), arrayOf(ConfigurationValues.CONFIGURATION_VALUES.ID), true)
val PK_ENVIRONMENTS: UniqueKey<EnvironmentsRecord> = Internal.createUniqueKey(Environments.ENVIRONMENTS, DSL.name("PK_ENVIRONMENTS"), arrayOf(Environments.ENVIRONMENTS.ID), true)
val PK_SERVICES: UniqueKey<ServicesRecord> = Internal.createUniqueKey(Services.SERVICES, DSL.name("PK_SERVICES"), arrayOf(Services.SERVICES.ID), true)
val PK_TENANTS: UniqueKey<TenantsRecord> = Internal.createUniqueKey(Tenants.TENANTS, DSL.name("PK_TENANTS"), arrayOf(Tenants.TENANTS.ID), true)
val PK_USERS: UniqueKey<UsersRecord> = Internal.createUniqueKey(Users.USERS, DSL.name("PK_USERS"), arrayOf(Users.USERS.ID), true)

// -------------------------------------------------------------------------
// FOREIGN KEY definitions
// -------------------------------------------------------------------------

val FK_ENVIRONMENTS_CLOUD_ID: ForeignKey<EnvironmentsRecord, CloudsRecord> = Internal.createForeignKey(Environments.ENVIRONMENTS, DSL.name("FK_ENVIRONMENTS_CLOUD_ID"), arrayOf(Environments.ENVIRONMENTS.CLOUD), de.solidblocks.config.db.keys.PK_CLOUDS, arrayOf(Clouds.CLOUDS.ID), true)
val FK_SERVICES_ENVIRONMENT_ID: ForeignKey<ServicesRecord, EnvironmentsRecord> = Internal.createForeignKey(Services.SERVICES, DSL.name("FK_SERVICES_ENVIRONMENT_ID"), arrayOf(Services.SERVICES.ENVIRONMENT), de.solidblocks.config.db.keys.PK_ENVIRONMENTS, arrayOf(Environments.ENVIRONMENTS.ID), true)
val FK_TENANTS_ENVIRONMENT_ID: ForeignKey<TenantsRecord, EnvironmentsRecord> = Internal.createForeignKey(Tenants.TENANTS, DSL.name("FK_TENANTS_ENVIRONMENT_ID"), arrayOf(Tenants.TENANTS.ENVRIONMENT), de.solidblocks.config.db.keys.PK_ENVIRONMENTS, arrayOf(Environments.ENVIRONMENTS.ID), true)
val FK_USERS_CLOUDS_ID: ForeignKey<UsersRecord, CloudsRecord> = Internal.createForeignKey(Users.USERS, DSL.name("FK_USERS_CLOUDS_ID"), arrayOf(Users.USERS.CLOUD), de.solidblocks.config.db.keys.PK_CLOUDS, arrayOf(Clouds.CLOUDS.ID), true)
val FK_USERS_ENVIRONMENT_ID: ForeignKey<UsersRecord, EnvironmentsRecord> = Internal.createForeignKey(Users.USERS, DSL.name("FK_USERS_ENVIRONMENT_ID"), arrayOf(Users.USERS.ENVIRONMENT), de.solidblocks.config.db.keys.PK_ENVIRONMENTS, arrayOf(Environments.ENVIRONMENTS.ID), true)
val FK_USERS_TENANTS_ID: ForeignKey<UsersRecord, TenantsRecord> = Internal.createForeignKey(Users.USERS, DSL.name("FK_USERS_TENANTS_ID"), arrayOf(Users.USERS.TENANT), de.solidblocks.config.db.keys.PK_TENANTS, arrayOf(Tenants.TENANTS.ID), true)
