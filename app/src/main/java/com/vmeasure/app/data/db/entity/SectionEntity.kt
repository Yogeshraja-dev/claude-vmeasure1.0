package com.vmeasure.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "measurement_sections",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class SectionEntity(
    @PrimaryKey val sectionId: String,
    val userId: String,
    val type: String,          // "Blouse", "Kurti", etc.
    val createdAt: String = "",
    val updatedAt: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val sortOrder: Int = 0,    // preserves creation order within a tag

    // Blouse + Kurti shared
    val uBust: String = "",
    val bust: String = "",
    val waist: String = "",
    val hip: String = "",
    val armhole: String = "",
    val shoulder: String = "",
    val length: String = "",
    val fNeck: String = "",
    val bNeck: String = "",
    val sleeveLength: String = "",
    val sleeveRound: String = "",

    // Kurti only
    val blouseCut: String = "",
    val blouseField: String = "",  // "Blouse" field inside Kurti section

    // Pant
    val thighRound: String = "",
    val kneeRound: String = "",
    val bottom: String = "",
    val inseam: String = "",

    // Frock
    val frockLength: String = "",
    val yokeLength: String = "",

    // Crop Blouse & Skirt
    val blouseWaist: String = "",
    val blouseLength: String = "",
    val skirtLength: String = "",
    val waistLength: String = "",

    // Kids Boy
    val chest: String = "",
    val pantLength: String = "",
    val pantWaist: String = "",

    // All sections
    val notes: String = ""
)