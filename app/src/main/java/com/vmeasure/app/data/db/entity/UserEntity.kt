package com.vmeasure.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val dateOfBirth: String = "",
    val specialDate: String = "",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val contactNumber: String = "",
    val instagramId: String = "",
    val otherMediaId: String = "",
    val location: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    /** JSON string: e.g. {"Blouse":2,"Kurti":1} */
    val selectedTags: String = "{}",
    /** Unix millis for sorting — set from createdAt timestamp */
    val createdAtMillis: Long = 0L,
    /** Unix millis for sorting — 0 if never updated */
    val updatedAtMillis: Long = 0L,
    /** Unix millis for specialDate filtering */
    val specialDateMillis: Long = 0L,
    /** Unix millis for dateOfBirth filtering */
    val dobMillis: Long = 0L
)