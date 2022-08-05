package de.solidblocks.cloud.model.repositories

import de.solidblocks.config.db.tables.references.STATUS
import org.jooq.DSLContext
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

class StatusRepository(dsl: DSLContext) : BaseRepository(dsl) {

    fun updateStatus(entityId: UUID, status: String, code: String) {
        dsl.insertInto(STATUS).columns(STATUS.ID, STATUS.ENTITY, STATUS.STATUS_, STATUS.CODE).values(UUID.randomUUID(), entityId, status, code).execute()
    }

    fun cleanupEphemeralData() {
        dsl.deleteFrom(STATUS).execute()
    }

    fun latestStatus(entityId: UUID, interval: Duration) = dsl.selectFrom(STATUS).where(STATUS.ENTITY.eq(entityId)).and(STATUS.STATUS_TIMESTAMP.ge(LocalDateTime.now().minus(interval))).orderBy(STATUS.STATUS_TIMESTAMP.desc()).limit(1).offset(0).fetchOne(STATUS.STATUS_)
}
