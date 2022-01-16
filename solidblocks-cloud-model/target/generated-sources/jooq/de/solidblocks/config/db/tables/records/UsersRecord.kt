/*
 * This file is generated by jOOQ.
 */
package de.solidblocks.config.db.tables.records


import de.solidblocks.config.db.tables.Users

import java.util.UUID

import org.jooq.Field
import org.jooq.Record1
import org.jooq.Record9
import org.jooq.Row9
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class UsersRecord() : UpdatableRecordImpl<UsersRecord>(Users.USERS), Record9<UUID?, String?, String?, String?, Boolean?, Boolean?, UUID?, UUID?, UUID?> {

    var id: UUID?
        set(value) = set(0, value)
        get() = get(0) as UUID?

    var email: String?
        set(value) = set(1, value)
        get() = get(1) as String?

    var password: String?
        set(value) = set(2, value)
        get() = get(2) as String?

    var salt: String?
        set(value) = set(3, value)
        get() = get(3) as String?

    var admin: Boolean?
        set(value) = set(4, value)
        get() = get(4) as Boolean?

    var deleted: Boolean?
        set(value) = set(5, value)
        get() = get(5) as Boolean?

    var cloud: UUID?
        set(value) = set(6, value)
        get() = get(6) as UUID?

    var environment: UUID?
        set(value) = set(7, value)
        get() = get(7) as UUID?

    var tenant: UUID?
        set(value) = set(8, value)
        get() = get(8) as UUID?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<UUID?> = super.key() as Record1<UUID?>

    // -------------------------------------------------------------------------
    // Record9 type implementation
    // -------------------------------------------------------------------------

    override fun fieldsRow(): Row9<UUID?, String?, String?, String?, Boolean?, Boolean?, UUID?, UUID?, UUID?> = super.fieldsRow() as Row9<UUID?, String?, String?, String?, Boolean?, Boolean?, UUID?, UUID?, UUID?>
    override fun valuesRow(): Row9<UUID?, String?, String?, String?, Boolean?, Boolean?, UUID?, UUID?, UUID?> = super.valuesRow() as Row9<UUID?, String?, String?, String?, Boolean?, Boolean?, UUID?, UUID?, UUID?>
    override fun field1(): Field<UUID?> = Users.USERS.ID
    override fun field2(): Field<String?> = Users.USERS.EMAIL
    override fun field3(): Field<String?> = Users.USERS.PASSWORD
    override fun field4(): Field<String?> = Users.USERS.SALT
    override fun field5(): Field<Boolean?> = Users.USERS.ADMIN
    override fun field6(): Field<Boolean?> = Users.USERS.DELETED
    override fun field7(): Field<UUID?> = Users.USERS.CLOUD
    override fun field8(): Field<UUID?> = Users.USERS.ENVIRONMENT
    override fun field9(): Field<UUID?> = Users.USERS.TENANT
    override fun component1(): UUID? = id
    override fun component2(): String? = email
    override fun component3(): String? = password
    override fun component4(): String? = salt
    override fun component5(): Boolean? = admin
    override fun component6(): Boolean? = deleted
    override fun component7(): UUID? = cloud
    override fun component8(): UUID? = environment
    override fun component9(): UUID? = tenant
    override fun value1(): UUID? = id
    override fun value2(): String? = email
    override fun value3(): String? = password
    override fun value4(): String? = salt
    override fun value5(): Boolean? = admin
    override fun value6(): Boolean? = deleted
    override fun value7(): UUID? = cloud
    override fun value8(): UUID? = environment
    override fun value9(): UUID? = tenant

    override fun value1(value: UUID?): UsersRecord {
        this.id = value
        return this
    }

    override fun value2(value: String?): UsersRecord {
        this.email = value
        return this
    }

    override fun value3(value: String?): UsersRecord {
        this.password = value
        return this
    }

    override fun value4(value: String?): UsersRecord {
        this.salt = value
        return this
    }

    override fun value5(value: Boolean?): UsersRecord {
        this.admin = value
        return this
    }

    override fun value6(value: Boolean?): UsersRecord {
        this.deleted = value
        return this
    }

    override fun value7(value: UUID?): UsersRecord {
        this.cloud = value
        return this
    }

    override fun value8(value: UUID?): UsersRecord {
        this.environment = value
        return this
    }

    override fun value9(value: UUID?): UsersRecord {
        this.tenant = value
        return this
    }

    override fun values(value1: UUID?, value2: String?, value3: String?, value4: String?, value5: Boolean?, value6: Boolean?, value7: UUID?, value8: UUID?, value9: UUID?): UsersRecord {
        this.value1(value1)
        this.value2(value2)
        this.value3(value3)
        this.value4(value4)
        this.value5(value5)
        this.value6(value6)
        this.value7(value7)
        this.value8(value8)
        this.value9(value9)
        return this
    }

    /**
     * Create a detached, initialised UsersRecord
     */
    constructor(id: UUID? = null, email: String? = null, password: String? = null, salt: String? = null, admin: Boolean? = null, deleted: Boolean? = null, cloud: UUID? = null, environment: UUID? = null, tenant: UUID? = null): this() {
        this.id = id
        this.email = email
        this.password = password
        this.salt = salt
        this.admin = admin
        this.deleted = deleted
        this.cloud = cloud
        this.environment = environment
        this.tenant = tenant
    }
}
