package com.vmeasure.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val key: String,
    val value: String
)

object ConfigKeys {
    const val DEVICE_ID = "deviceId"
    const val GOOGLE_ACCOUNT_EMAIL = "googleAccountEmail"
    const val GOOGLE_ACCOUNT_TOKEN = "googleAccountToken"
    const val LAST_SYNC_TIME = "lastSyncTime"
    const val DRIVE_FOLDER_ID = "driveFolderId"
    const val APP_VERSION = "appVersion"
    const val DB_VERSION = "databaseVersion"
}