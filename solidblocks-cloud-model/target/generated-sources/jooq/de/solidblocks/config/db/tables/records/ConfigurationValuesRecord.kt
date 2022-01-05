/*
 * This file is generated by jOOQ.
 */
package de.solidblocks.config.db.tables.records

import de.solidblocks.config.db.tables.ConfigurationValues
import org.jooq.Field
import org.jooq.Record1
import org.jooq.Record8
import org.jooq.Row8
import org.jooq.impl.UpdatableRecordImpl
import java.util.UUID

/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class ConfigurationValuesRecord() : UpdatableRecordImpl<ConfigurationValuesRecord>(ConfigurationValues.CONFIGURATION_VALUES), Record8<UUID?, Int?, String?, String?, UUID?, UUID?, UUID?, UUID?> {

    var id: UUID?
        set(value) = set(0, value)
        get() = get(0) as UUID?

    var version: Int?
        set(value) = set(1, value)
        get() = get(1) as Int?

    var name: String?
        set(value) = set(2, value)
        get() = get(2) as String?

    var configValue: String?
        set(value) = set(3, value)
        get() = get(3) as String?

    var tenant: UUID?
        set(value) = set(4, value)
        get() = get(4) as UUID?

    var service: UUID?
        set(value) = set(5, value)
        get() = get(5) as UUID?

    var environment: UUID?
        set(value) = set(6, value)
        get() = get(6) as UUID?

    var cloud: UUID?
        set(value) = set(7, value)
        get() = get(7) as UUID?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<UUID?> = super.key() as Record1<UUID?>

    // -------------------------------------------------------------------------
    // Record8 type implementation
    // -------------------------------------------------------------------------

    override fun fieldsRow(): Row8<UUID?, Int?, String?, String?, UUID?, UUID?, UUID?, UUID?> = super.fieldsRow() as Row8<UUID?, Int?, String?, String?, UUID?, UUID?, UUID?, UUID?>
    override fun valuesRow(): Row8<UUID?, Int?, String?, String?, UUID?, UUID?, UUID?, UUID?> = super.valuesRow() as Row8<UUID?, Int?, String?, String?, UUID?, UUID?, UUID?, UUID?>
    override fun field1(): Field<UUID?> = ConfigurationValues.CONFIGURATION_VALUES.ID
    override fun field2(): Field<Int?> = ConfigurationValues.CONFIGURATION_VALUES.VERSION
    override fun field3(): Field<String?> = ConfigurationValues.CONFIGURATION_VALUES.NAME
    override fun field4(): Field<String?> = ConfigurationValues.CONFIGURATION_VALUES.CONFIG_VALUE
    override fun field5(): Field<UUID?> = ConfigurationValues.CONFIGURATION_VALUES.TENANT
    override fun field6(): Field<UUID?> = ConfigurationValues.CONFIGURATION_VALUES.SERVICE
    override fun field7(): Field<UUID?> = ConfigurationValues.CONFIGURATION_VALUES.ENVIRONMENT
    override fun field8(): Field<UUID?> = ConfigurationValues.CONFIGURATION_VALUES.CLOUD
    override fun component1(): UUID? = id
    override fun component2(): Int? = version
    override fun component3(): String? = name
    override fun component4(): String? = configValue
    override fun component5(): UUID? = tenant
    override fun component6(): UUID? = service
    override fun component7(): UUID? = environment
    override fun component8(): UUID? = cloud
    override fun value1(): UUID? = id
    override fun value2(): Int? = version
    override fun value3(): String? = name
    override fun value4(): String? = configValue
    override fun value5(): UUID? = tenant
    override fun value6(): UUID? = service
    override fun value7(): UUID? = environment
    override fun value8(): UUID? = cloud

    override fun value1(value: UUID?): ConfigurationValuesRecord {
        this.id = value
        return this
    }

    override fun value2(value: Int?): ConfigurationValuesRecord {
        this.version = value
        return this
    }

    override fun value3(value: String?): ConfigurationValuesRecord {
        this.name = value
        return this
    }

    override fun value4(value: String?): ConfigurationValuesRecord {
        this.configValue = value
        return this
    }

    override fun value5(value: UUID?): ConfigurationValuesRecord {
        this.tenant = value
        return this
    }

    override fun value6(value: UUID?): ConfigurationValuesRecord {
        this.service = value
        return this
    }

    override fun value7(value: UUID?): ConfigurationValuesRecord {
        this.environment = value
        return this
    }

    override fun value8(value: UUID?): ConfigurationValuesRecord {
        this.cloud = value
        return this
    }

    override fun values(value1: UUID?, value2: Int?, value3: String?, value4: String?, value5: UUID?, value6: UUID?, value7: UUID?, value8: UUID?): ConfigurationValuesRecord {
        this.value1(value1)
        this.value2(value2)
        this.value3(value3)
        this.value4(value4)
        this.value5(value5)
        this.value6(value6)
        this.value7(value7)
        this.value8(value8)
        return this
    }

    /**
     * Create a detached, initialised ConfigurationValuesRecord
     */
    constructor(id: UUID? = null, version: Int? = null, name: String? = null, configValue: String? = null, tenant: UUID? = null, service: UUID? = null, environment: UUID? = null, cloud: UUID? = null) : this() {
        this.id = id
        this.version = version
        this.name = name
        this.configValue = configValue
        this.tenant = tenant
        this.service = service
        this.environment = environment
        this.cloud = cloud
    }
}