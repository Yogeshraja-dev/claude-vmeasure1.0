package com.vmeasure.app.data.model

data class FilterState(
    val sortMode: SortMode = SortMode.RECENT,
    val customDateFrom: Long = 0L,
    val customDateTo: Long = 0L,
    val selectedTags: Set<String> = emptySet(),
    val favouriteOnly: Boolean = false,
    val pinnedOnly: Boolean = false,
    val specialDateFrom: Long = 0L,
    val specialDateTo: Long = 0L,
    val birthDateFrom: Long = 0L,
    val birthDateTo: Long = 0L
) {
    val isDefault: Boolean
        get() = sortMode == SortMode.RECENT &&
                customDateFrom == 0L &&
                customDateTo == 0L &&
                selectedTags.isEmpty() &&
                !favouriteOnly &&
                !pinnedOnly &&
                specialDateFrom == 0L &&
                specialDateTo == 0L &&
                birthDateFrom == 0L &&
                birthDateTo == 0L
}

enum class SortMode {
    CUSTOM_DATE,
    RECENT,     // newest updatedAt first
    OLDEST,     // oldest updatedAt first
    AZ,
    ZA
}