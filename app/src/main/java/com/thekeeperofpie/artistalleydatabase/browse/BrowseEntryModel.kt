package com.thekeeperofpie.artistalleydatabase.browse

data class BrowseEntryModel(
    val image: String? = null,
    val text: String,
    val query: String = text,
)