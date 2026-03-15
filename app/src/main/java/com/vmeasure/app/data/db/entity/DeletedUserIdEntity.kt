package com.vmeasure.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_user_ids")
data class DeletedUserIdEntity(
    @PrimaryKey val userId: String
)