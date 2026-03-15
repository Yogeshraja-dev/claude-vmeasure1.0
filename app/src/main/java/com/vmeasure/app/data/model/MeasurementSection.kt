package com.vmeasure.app.data.model

data class MeasurementSection(
    val sectionId: String,
    val userId: String,
    val type: String,
    val createdAt: String,
    val updatedAt: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val sortOrder: Int,

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

    // Kurti extras
    val blouseCut: String = "",
    val blouseField: String = "",

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