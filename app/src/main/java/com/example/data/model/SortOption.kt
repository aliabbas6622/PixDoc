package com.example.data.model

enum class SortField(val label: String) {
    NAME("Name"),
    DATE("Date Modified"),
    SIZE("Size"),
    TYPE("Type")
}

enum class SortOrder(val label: String) {
    ASCENDING("A to Z / Oldest / Smallest"),
    DESCENDING("Z to A / Newest / Largest")
}

enum class ViewMode {
    LIST,
    GRID
}

data class SortOption(
    val field: SortField = SortField.NAME,
    val ascending: Boolean = true
) {
    fun toggleOrder(): SortOption = copy(ascending = !ascending)
}
