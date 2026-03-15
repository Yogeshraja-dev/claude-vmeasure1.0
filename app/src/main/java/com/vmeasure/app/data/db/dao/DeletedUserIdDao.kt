package com.vmeasure.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vmeasure.app.data.db.entity.DeletedUserIdEntity

@Dao
interface DeletedUserIdDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: DeletedUserIdEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<DeletedUserIdEntity>)

    @Query("SELECT userId FROM deleted_user_ids")
    suspend fun getAllDeletedIds(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM deleted_user_ids WHERE userId = :userId)")
    suspend fun isDeleted(userId: String): Boolean
}