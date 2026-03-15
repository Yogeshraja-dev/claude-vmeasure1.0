package com.vmeasure.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vmeasure.app.data.db.entity.AppConfigEntity

@Dao
interface AppConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(entity: AppConfigEntity)

    @Query("SELECT value FROM app_config WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Query("DELETE FROM app_config WHERE `key` = :key")
    suspend fun delete(key: String)
}