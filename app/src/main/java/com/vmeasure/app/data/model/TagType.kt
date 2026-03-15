package com.vmeasure.app.data.model

enum class TagType(val displayName: String) {
    BLOUSE("Blouse"),
    KURTI("Kurti"),
    PANT("Pant"),
    FROCK("Frock"),
    CROP_BLOUSE_AND_SKIRT("Crop Blouse and Skirt"),
    KIDS_BOY("Kids Boy");

    companion object {
        val orderedList = listOf(BLOUSE, KURTI, PANT, FROCK, CROP_BLOUSE_AND_SKIRT, KIDS_BOY)

        fun fromDisplayName(name: String): TagType? =
            values().find { it.displayName == name }
    }
}