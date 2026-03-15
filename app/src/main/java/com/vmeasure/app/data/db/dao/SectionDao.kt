package com.vmeasure.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vmeasure.app.data.db.entity.SectionEntity

@Dao
interface SectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: SectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSections(sections: List<SectionEntity>)

    @Update
    suspend fun updateSection(section: SectionEntity)

    @Query("SELECT * FROM measurement_sections WHERE userId = :userId ORDER BY type ASC, sortOrder ASC")
    suspend fun getSectionsForUser(userId: String): List<SectionEntity>

    @Query("SELECT * FROM measurement_sections WHERE sectionId = :sectionId")
    suspend fun getSectionById(sectionId: String): SectionEntity?

    @Query("DELETE FROM measurement_sections WHERE sectionId = :sectionId")
    suspend fun deleteSection(sectionId: String)

    @Query("DELETE FROM measurement_sections WHERE userId = :userId")
    suspend fun deleteSectionsForUser(userId: String)

    @Query("SELECT COUNT(*) FROM measurement_sections WHERE userId = :userId AND type = :type")
    suspend fun countSectionsForTag(userId: String, type: String): Int

    @Query("SELECT MAX(sortOrder) FROM measurement_sections WHERE userId = :userId AND type = :type")
    suspend fun maxSortOrderForTag(userId: String, type: String): Int?
}