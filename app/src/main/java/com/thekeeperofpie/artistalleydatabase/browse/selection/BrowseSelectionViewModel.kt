package com.thekeeperofpie.artistalleydatabase.browse.selection

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.thekeeperofpie.artistalleydatabase.art.ArtEntryColumn
import com.thekeeperofpie.artistalleydatabase.art.grid.ArtEntryGridModel
import com.thekeeperofpie.artistalleydatabase.art.grid.ArtEntryGridViewModel
import com.thekeeperofpie.artistalleydatabase.browse.ArtEntryBrowseDao
import com.thekeeperofpie.artistalleydatabase.json.AppJson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseSelectionViewModel @Inject constructor(
    application: Application,
    private val artEntryBrowseDao: ArtEntryBrowseDao,
    private val appJson: AppJson,
) : ArtEntryGridViewModel(application, artEntryBrowseDao) {

    lateinit var column: ArtEntryColumn

    var loading by mutableStateOf(false)
    val entries = MutableStateFlow(PagingData.empty<ArtEntryGridModel>())

    fun initialize(column: ArtEntryColumn, query: String) {
        if (this::column.isInitialized) return
        this.column = column

        viewModelScope.launch(Dispatchers.IO) {
            Pager(PagingConfig(pageSize = 20)) {
                when (column) {
                    ArtEntryColumn.ARTISTS -> artEntryBrowseDao.getArtist(query)
                    ArtEntryColumn.SOURCE -> TODO()
                    ArtEntryColumn.SERIES -> artEntryBrowseDao.getSeries(query)
                    ArtEntryColumn.CHARACTERS -> artEntryBrowseDao.getCharacter(query)
                    ArtEntryColumn.TAGS -> artEntryBrowseDao.getTag(query)
                }
            }
                .flow.cachedIn(viewModelScope)
                .map {
                    it.filter {
                        when (column) {
                            ArtEntryColumn.ARTISTS -> it.artists.contains(query)
                            ArtEntryColumn.SOURCE -> TODO()
                            ArtEntryColumn.SERIES -> it.series.any { it.contains(query) }
                            ArtEntryColumn.CHARACTERS -> it.characters.any { it.contains(query) }
                            ArtEntryColumn.TAGS -> it.tags.contains(query)
                        }
                    }
                        .map { ArtEntryGridModel.buildFromEntry(application, appJson, it) }
                }
                .onEach {
                    if (loading) {
                        launch(Dispatchers.Main) {
                            loading = false
                        }
                    }
                }
                .collect(entries)
        }
    }

    fun onDeleteSelected() {
        super.deleteSelected()
    }
}