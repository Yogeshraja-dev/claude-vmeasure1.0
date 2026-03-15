package com.vmeasure.app.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.vmeasure.app.data.db.dao.DeletedUserIdDao
import com.vmeasure.app.data.db.dao.SectionDao
import com.vmeasure.app.data.db.dao.UserDao
import com.vmeasure.app.data.db.entity.DeletedUserIdEntity
import com.vmeasure.app.data.db.entity.UserEntity
import com.vmeasure.app.data.model.FilterState
import com.vmeasure.app.data.model.MeasurementSection
import com.vmeasure.app.data.model.SortMode
import com.vmeasure.app.data.model.User
import com.vmeasure.app.util.DateFormatter
import com.vmeasure.app.util.IdGenerator
import com.vmeasure.app.util.toDomain
import com.vmeasure.app.util.toEntity
import com.vmeasure.app.util.toJson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val sectionDao: SectionDao,
    private val deletedUserIdDao: DeletedUserIdDao
) {

    // ── Fetch list with search + filter ───────────────────────────────────────

    suspend fun getUsers(
        searchQuery: String,
        filter: FilterState,
        limit: Int,
        offset: Int
    ): List<User> {
        val sql = buildQuery(searchQuery, filter, limit, offset)
        val entities = userDao.getUsersRaw(SimpleSQLiteQuery(sql.first, sql.second.toTypedArray()))
        return entities.map { it.toDomain() }
    }

    suspend fun getTotalCount(): Int = userDao.getTotalCount()

    // ── Build dynamic SQL ─────────────────────────────────────────────────────

    private fun buildQuery(
        search: String,
        filter: FilterState,
        limit: Int,
        offset: Int
    ): Pair<String, List<Any>> {
        val args = mutableListOf<Any>()
        val where = mutableListOf<String>()

        // Search
        if (search.isNotBlank()) {
            where.add("(LOWER(name) LIKE ? OR contactNumber LIKE ?)")
            val pattern = "%${search.trim().lowercase()}%"
            args.add(pattern)
            args.add("%${search.trim()}%")
        }

        // Favourite filter
        if (filter.favouriteOnly) {
            where.add("isFavorite = 1")
        }

        // Pinned filter
        if (filter.pinnedOnly) {
            where.add("isPinned = 1")
        }

        // Special date range
        if (filter.specialDateFrom > 0L) {
            where.add("specialDateMillis >= ?")
            args.add(filter.specialDateFrom)
        }
        if (filter.specialDateTo > 0L) {
            where.add("specialDateMillis <= ?")
            args.add(DateFormatter.endOfDayMillis(filter.specialDateTo))
        }

        // Birth date range
        if (filter.birthDateFrom > 0L) {
            where.add("dobMillis >= ?")
            args.add(filter.birthDateFrom)
        }
        if (filter.birthDateTo > 0L) {
            where.add("dobMillis <= ?")
            args.add(DateFormatter.endOfDayMillis(filter.birthDateTo))
        }

        // Custom update date range — exclude users with empty updatedAt
        if (filter.sortMode == SortMode.CUSTOM_DATE) {
            val from = filter.customDateFrom
            val to = if (filter.customDateTo > 0L) filter.customDateTo
            else DateFormatter.endOfTodayMillis()
            if (from > 0L) {
                where.add("updatedAtMillis > 0")
                where.add("updatedAtMillis >= ?")
                args.add(from)
                where.add("updatedAtMillis <= ?")
                args.add(DateFormatter.endOfDayMillis(to))
            }
        }

        // Tag type filter — OR condition using selectedTags JSON
        if (filter.selectedTags.isNotEmpty()) {
            val tagConditions = filter.selectedTags.map { tag ->
                "selectedTags LIKE ?"
            }
            where.add("(${tagConditions.joinToString(" OR ")})")
            filter.selectedTags.forEach { tag ->
                args.add("%\"$tag\"%")
            }
        }

        val whereClause = if (where.isEmpty()) "" else "WHERE ${where.joinToString(" AND ")}"

        // Order by: pinned first always, then by filter sort mode
        val orderBy = buildOrderBy(filter.sortMode)

        val sql = """
            SELECT * FROM users
            $whereClause
            ORDER BY $orderBy
            LIMIT $limit OFFSET $offset
        """.trimIndent()

        return Pair(sql, args)
    }

    private fun buildOrderBy(sortMode: SortMode): String {
        val pinnedFirst = "isPinned DESC"
        return when (sortMode) {
            SortMode.RECENT      -> "$pinnedFirst, CASE WHEN updatedAtMillis > 0 THEN updatedAtMillis ELSE createdAtMillis END DESC"
            SortMode.OLDEST      -> "$pinnedFirst, CASE WHEN updatedAtMillis > 0 THEN updatedAtMillis ELSE createdAtMillis END ASC"
            SortMode.AZ          -> "$pinnedFirst, LOWER(name) ASC, createdAtMillis ASC"
            SortMode.ZA          -> "$pinnedFirst, LOWER(name) DESC, createdAtMillis ASC"
            SortMode.CUSTOM_DATE -> "$pinnedFirst, updatedAtMillis DESC"
        }
    }

    // ── Get single user with sections ─────────────────────────────────────────

    suspend fun getUserWithSections(userId: String): User? {
        val entity = userDao.getUserById(userId) ?: return null
        val sections = sectionDao.getSectionsForUser(userId).map { it.toDomain() }
        return entity.toDomain(sections)
    }

    // ── Save new user ─────────────────────────────────────────────────────────

    suspend fun saveNewUser(user: User, sections: List<MeasurementSection>) {
        val now = DateFormatter.nowTimestamp()
        val nowMillis = DateFormatter.nowMillis()

        val tagMap = sections.groupBy { it.type }
            .mapValues { it.value.size }

        val entity = UserEntity(
            userId = user.userId,
            name = user.name.trim(),
            dateOfBirth = user.dateOfBirth,
            specialDate = user.specialDate,
            isFavorite = user.isFavorite,
            isPinned = user.isPinned,
            contactNumber = user.contactNumber,
            instagramId = user.instagramId,
            otherMediaId = user.otherMediaId,
            location = user.location,
            createdAt = now,
            updatedAt = "",
            selectedTags = tagMap.toJson(),
            createdAtMillis = nowMillis,
            updatedAtMillis = 0L,
            specialDateMillis = DateFormatter.parseDateToMillis(user.specialDate),
            dobMillis = DateFormatter.parseDateToMillis(user.dateOfBirth)
        )

        userDao.insertUser(entity)

        val sectionEntities = sections.mapIndexed { index, section ->
            section.copy(
                userId = user.userId,
                sortOrder = index
            ).toEntity()
        }
        sectionDao.insertSections(sectionEntities)
    }

    // ── Update existing user ──────────────────────────────────────────────────

    suspend fun updateUser(
        user: User,
        sections: List<MeasurementSection>,
        changedSectionIds: Set<String>
    ) {
        val now = DateFormatter.nowTimestamp()
        val nowMillis = DateFormatter.nowMillis()

        val tagMap = sections.groupBy { it.type }
            .mapValues { it.value.size }

        val existing = userDao.getUserById(user.userId) ?: return

        val updated = existing.copy(
            name = user.name.trim(),
            dateOfBirth = user.dateOfBirth,
            specialDate = user.specialDate,
            isFavorite = user.isFavorite,
            isPinned = user.isPinned,
            contactNumber = user.contactNumber,
            instagramId = user.instagramId,
            otherMediaId = user.otherMediaId,
            location = user.location,
            updatedAt = now,
            updatedAtMillis = nowMillis,
            selectedTags = tagMap.toJson(),
            specialDateMillis = DateFormatter.parseDateToMillis(user.specialDate),
            dobMillis = DateFormatter.parseDateToMillis(user.dateOfBirth)
        )

        userDao.updateUser(updated)

        // Delete all existing sections and re-insert (handles deletes + order)
        sectionDao.deleteSectionsForUser(user.userId)

        val sectionEntities = sections.mapIndexed { index, section ->
            val isChanged = section.sectionId in changedSectionIds
            section.copy(
                userId = user.userId,
                sortOrder = index,
                updatedAt = if (isChanged) now else section.updatedAt,
                updatedAtMillis = if (isChanged) nowMillis else section.updatedAtMillis
            ).toEntity()
        }
        sectionDao.insertSections(sectionEntities)
    }

    // ── Toggle favourite (no updatedAt change) ────────────────────────────────

    suspend fun toggleFavourite(userId: String, isFavorite: Boolean) {
        userDao.updateFavorite(userId, isFavorite)
    }

    // ── Toggle pin (no updatedAt change) ─────────────────────────────────────

    suspend fun togglePin(userId: String, isPinned: Boolean) {
        userDao.updatePinned(userId, isPinned)
    }

    // ── Delete single user ────────────────────────────────────────────────────

    suspend fun deleteUser(userId: String) {
        userDao.deleteUser(userId)
        deletedUserIdDao.insert(DeletedUserIdEntity(userId))
    }

    // ── Delete multiple users ─────────────────────────────────────────────────

    suspend fun deleteUsers(userIds: List<String>) {
        userDao.deleteUsers(userIds)
        val entities = userIds.map { DeletedUserIdEntity(it) }
        deletedUserIdDao.insertAll(entities)
    }

    // ── Get all deleted IDs ───────────────────────────────────────────────────

    suspend fun getAllDeletedIds(): List<String> =
        deletedUserIdDao.getAllDeletedIds()

    // ── Check if userId is deleted ────────────────────────────────────────────

    suspend fun isDeleted(userId: String): Boolean =
        deletedUserIdDao.isDeleted(userId)

    // ── Get all user IDs ──────────────────────────────────────────────────────

    suspend fun getAllUserIds(): List<String> =
        userDao.getAllUserIds()

    // ── Get sections for user ─────────────────────────────────────────────────

    suspend fun getSectionsForUser(userId: String): List<MeasurementSection> =
        sectionDao.getSectionsForUser(userId).map { it.toDomain() }

    // ── Upsert from sync ──────────────────────────────────────────────────────

    suspend fun upsertUserFromSync(user: User, sections: List<MeasurementSection>) {
        val tagMap = sections.groupBy { it.type }.mapValues { it.value.size }
        val entity = UserEntity(
            userId = user.userId,
            name = user.name,
            dateOfBirth = user.dateOfBirth,
            specialDate = user.specialDate,
            isFavorite = user.isFavorite,
            isPinned = user.isPinned,
            contactNumber = user.contactNumber,
            instagramId = user.instagramId,
            otherMediaId = user.otherMediaId,
            location = user.location,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
            selectedTags = tagMap.toJson(),
            createdAtMillis = DateFormatter.parseTimestampToMillis(user.createdAt)
                .takeIf { it > 0 } ?: DateFormatter.parseDateToMillis(user.createdAt),
            updatedAtMillis = DateFormatter.parseTimestampToMillis(user.updatedAt),
            specialDateMillis = DateFormatter.parseDateToMillis(user.specialDate),
            dobMillis = DateFormatter.parseDateToMillis(user.dateOfBirth)
        )
        userDao.insertUser(entity)
        sectionDao.deleteSectionsForUser(user.userId)
        sectionDao.insertSections(
            sections.mapIndexed { i, s -> s.copy(userId = user.userId, sortOrder = i).toEntity() }
        )
    }
}