package com.vmeasure.app.data.model

data class User(
    val userId: String,
    val name: String,
    val dateOfBirth: String,
    val specialDate: String,
    val isFavorite: Boolean,
    val isPinned: Boolean,
    val contactNumber: String,
    val instagramId: String,
    val otherMediaId: String,
    val location: String,
    val createdAt: String,
    val updatedAt: String,
    val selectedTags: Map<String, Int>,   // tag name → section count
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val specialDateMillis: Long,
    val dobMillis: Long,
    val sections: List<MeasurementSection> = emptyList()
)