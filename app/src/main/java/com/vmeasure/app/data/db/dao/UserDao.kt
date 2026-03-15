package com.vmeasure.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.room.RawQuery
import com.vmeasure.app.data.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun observeUserById(userId: String): Flow<UserEntity?>

    /** Used for dynamic queries with filters */
    @RawQuery
    suspend fun getUsersRaw(query: SupportSQLiteQuery): List<UserEntity>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getTotalCount(): Int

    @Query("""
        UPDATE users SET isFavorite = :isFavorite WHERE userId = :userId
    """)
    suspend fun updateFavorite(userId: String, isFavorite: Boolean)

    @Query("""
        UPDATE users SET isPinned = :isPinned WHERE userId = :userId
    """)
    suspend fun updatePinned(userId: String, isPinned: Boolean)

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUser(userId: String)

    @Query("DELETE FROM users WHERE userId IN (:userIds)")
    suspend fun deleteUsers(userIds: List<String>)

    @Query("SELECT userId FROM users")
    suspend fun getAllUserIds(): List<String>

    @Query("""
        UPDATE users 
        SET selectedTags = :selectedTags 
        WHERE userId = :userId
    """)
    suspend fun updateSelectedTags(userId: String, selectedTags: String)
}