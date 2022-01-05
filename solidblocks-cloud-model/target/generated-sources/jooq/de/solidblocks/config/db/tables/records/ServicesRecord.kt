/*
 * This file is generated by jOOQ.
 */
package de.solidblocks.config.db.tables.records

import de.solidblocks.config.db.tables.Services
import org.jooq.Field
import org.jooq.Record1
import org.jooq.Record4
import org.jooq.Row4
import org.jooq.impl.UpdatableRecordImpl
import java.util.UUID

/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class ServicesRecord() : UpdatableRecordImpl<ServicesRecord>(Services.SERVICES), Record4<UUID?, String?, Boolean?, UUID?> {

    var id: UUID?
        set(value) = set(0, value)
        get() = get(0) as UUID?

    var name: String?
        set(value) = set(1, value)
        get() = get(1) as String?

    var deleted: Boolean?
        set(value) = set(2, value)
        get() = get(2) as Boolean?

    var environment: UUID?
        set(value) = set(3, value)
        get() = get(3) as UUID?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<UUID?> = super.key() as Record1<UUID?>

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    override fun fieldsRow(): Row4<UUID?, String?, Boolean?, UUID?> = super.fieldsRow() as Row4<UUID?, String?, Boolean?, UUID?>
    override fun valuesRow(): Row4<UUID?, String?, Boolean?, UUID?> = super.valuesRow() as Row4<UUID?, String?, Boolean?, UUID?>
    override fun field1(): Field<UUID?> = Services.SERVICES.ID
    override fun field2(): Field<String?> = Services.SERVICES.NAME
    override fun field3(): Field<Boolean?> = Services.SERVICES.DELETED
    override fun field4(): Field<UUID?> = Services.SERVICES.ENVIRONMENT
    override fun component1(): UUID? = id
    override fun component2(): String? = name
    override fun component3(): Boolean? = deleted
    override fun component4(): UUID? = environment
    override fun value1(): UUID? = id
    override fun value2(): String? = name
    override fun value3(): Boolean? = deleted
    override fun value4(): UUID? = environment

    override fun value1(value: UUID?): ServicesRecord {
        this.id = value
        return this
    }

    override fun value2(value: String?): ServicesRecord {
        this.name = value
        return this
    }

    override fun value3(value: Boolean?): ServicesRecord {
        this.deleted = value
        return this
    }

    override fun value4(value: UUID?): ServicesRecord {
        this.environment = value
        return this
    }

    override fun values(value1: UUID?, value2: String?, value3: Boolean?, value4: UUID?): ServicesRecord {
        this.value1(value1)
        this.value2(value2)
        this.value3(value3)
        this.value4(value4)
        return this
    }

    /**
     * Create a detached, initialised ServicesRecord
     */
    constructor(id: UUID? = null, name: String? = null, deleted: Boolean? = null, environment: UUID? = null) : this() {
        this.id = id
        this.name = name
        this.deleted = deleted
        this.environment = environment
    }
}