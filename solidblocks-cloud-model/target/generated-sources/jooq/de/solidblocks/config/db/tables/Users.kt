/*
 * This file is generated by jOOQ.
 */
package de.solidblocks.config.db.tables

import de.solidblocks.config.db.DefaultSchema
import de.solidblocks.config.db.keys.PK_USERS
import de.solidblocks.config.db.tables.records.UsersRecord
import org.jooq.Field
import org.jooq.ForeignKey
import org.jooq.Name
import org.jooq.Record
import org.jooq.Row5
import org.jooq.Schema
import org.jooq.Table
import org.jooq.TableField
import org.jooq.TableOptions
import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.Internal
import org.jooq.impl.SQLDataType
import org.jooq.impl.TableImpl
import java.util.UUID
import kotlin.collections.List

/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class Users(
    alias: Name,
    child: Table<out Record>?,
    path: ForeignKey<out Record, UsersRecord>?,
    aliased: Table<UsersRecord>?,
    parameters: Array<Field<*>?>?
) : TableImpl<UsersRecord>(
    alias,
    DefaultSchema.DEFAULT_SCHEMA,
    child,
    path,
    aliased,
    parameters,
    DSL.comment(""),
    TableOptions.table()
) {
    companion object {

        /**
         * The reference instance of <code>USERS</code>
         */
        val USERS = Users()
    }

    /**
     * The class holding records for this type
     */
    override fun getRecordType(): Class<UsersRecord> = UsersRecord::class.java

    /**
     * The column <code>USERS.ID</code>.
     */
    val ID: TableField<UsersRecord, UUID?> = createField(DSL.name("ID"), SQLDataType.UUID.nullable(false), this, "")

    /**
     * The column <code>USERS.EMAIL</code>.
     */
    val EMAIL: TableField<UsersRecord, String?> = createField(DSL.name("EMAIL"), SQLDataType.VARCHAR(256).nullable(false), this, "")

    /**
     * The column <code>USERS.PASSWORD</code>.
     */
    val PASSWORD: TableField<UsersRecord, String?> = createField(DSL.name("PASSWORD"), SQLDataType.VARCHAR(256).nullable(false), this, "")

    /**
     * The column <code>USERS.SALT</code>.
     */
    val SALT: TableField<UsersRecord, String?> = createField(DSL.name("SALT"), SQLDataType.VARCHAR(256).nullable(false), this, "")

    /**
     * The column <code>USERS.DELETED</code>.
     */
    val DELETED: TableField<UsersRecord, Boolean?> = createField(DSL.name("DELETED"), SQLDataType.BOOLEAN.nullable(false), this, "")

    private constructor(alias: Name, aliased: Table<UsersRecord>?) : this(alias, null, null, aliased, null)
    private constructor(alias: Name, aliased: Table<UsersRecord>?, parameters: Array<Field<*>?>?) : this(alias, null, null, aliased, parameters)

    /**
     * Create an aliased <code>USERS</code> table reference
     */
    constructor(alias: String) : this(DSL.name(alias))

    /**
     * Create an aliased <code>USERS</code> table reference
     */
    constructor(alias: Name) : this(alias, null)

    /**
     * Create a <code>USERS</code> table reference
     */
    constructor() : this(DSL.name("USERS"), null)

    constructor(child: Table<out Record>, key: ForeignKey<out Record, UsersRecord>) : this(Internal.createPathAlias(child, key), child, key, USERS, null)
    override fun getSchema(): Schema = DefaultSchema.DEFAULT_SCHEMA
    override fun getPrimaryKey(): UniqueKey<UsersRecord> = PK_USERS
    override fun getKeys(): List<UniqueKey<UsersRecord>> = listOf(PK_USERS)
    override fun `as`(alias: String): Users = Users(DSL.name(alias), this)
    override fun `as`(alias: Name): Users = Users(alias, this)

    /**
     * Rename this table
     */
    override fun rename(name: String): Users = Users(DSL.name(name), null)

    /**
     * Rename this table
     */
    override fun rename(name: Name): Users = Users(name, null)

    // -------------------------------------------------------------------------
    // Row5 type methods
    // -------------------------------------------------------------------------
    override fun fieldsRow(): Row5<UUID?, String?, String?, String?, Boolean?> = super.fieldsRow() as Row5<UUID?, String?, String?, String?, Boolean?>
}
