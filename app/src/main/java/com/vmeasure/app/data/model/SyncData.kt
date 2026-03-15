package com.vmeasure.app.data.model

import com.google.gson.annotations.SerializedName

data class SyncJson(
    @SerializedName("users") val users: List<SyncUser> = emptyList(),
    @SerializedName("deletedUserIds") val deletedUserIds: List<String> = emptyList(),
    @SerializedName("general") val general: SyncGeneral = SyncGeneral()
)

data class SyncUser(
    @SerializedName("id") val id: String,
    @SerializedName("userName") val userName: String,
    @SerializedName("dateOfBirth") val dateOfBirth: String = "",
    @SerializedName("specialDate") val specialDate: String = "",
    @SerializedName("isFavorite") val isFavorite: Boolean = false,
    @SerializedName("isPinned") val isPinned: Boolean = false,
    @SerializedName("contactNumber") val contactNumber: String = "",
    @SerializedName("instagramId") val instagramId: String = "",
    @SerializedName("otherMediaId") val otherMediaId: String = "",
    @SerializedName("location") val location: String = "",
    @SerializedName("createdAt") val createdAt: String = "",
    @SerializedName("updatedAt") val updatedAt: String = "",
    @SerializedName("tags") val tags: Map<String, Int> = emptyMap(),
    @SerializedName("measurementSections") val measurementSections: List<SyncSection> = emptyList()
)

data class SyncSection(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("createdAt") val createdAt: String = "",
    @SerializedName("updatedAt") val updatedAt: String = "",
    @SerializedName("data") val data: Map<String, String> = emptyMap()
)

data class SyncGeneral(
    @SerializedName("deviceId") val deviceId: String = "",
    @SerializedName("appVersion") val appVersion: String = "",
    @SerializedName("databaseVersion") val databaseVersion: String = "",
    @SerializedName("generalKey1") val generalKey1: String = "",
    @SerializedName("generalKey2") val generalKey2: String = ""
)