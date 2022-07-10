package com.thekeeperofpie.artistalleydatabase.anilist.media

import com.thekeeperofpie.artistalleydatabase.CustomApplication
import com.thekeeperofpie.artistalleydatabase.anilist.AniListApi
import com.thekeeperofpie.artistalleydatabase.utils.distinctWithBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MediaRepository(
    private val application: CustomApplication,
    private val mediaEntryDao: MediaEntryDao,
    private val aniListApi: AniListApi,
) {

    private val fetchMediaFlow = MutableStateFlow(-1)

    init {
        application.scope.launch(Dispatchers.IO) {
            fetchMediaFlow
                .drop(1) // Ignore initial value
                .distinctWithBuffer(10)
                .flatMapLatest { aniListApi.getMedia(it) }
                .mapNotNull { it?.aniListMedia }
                .map(MediaEntry::from)
                .collect(mediaEntryDao::insertEntries)
        }
    }

    suspend fun getEntry(id: Int) = mediaEntryDao.getEntry(id)
        .onEach { if (it == null) fetchMediaFlow.emit(id) }

    fun ensureSaved(id: Int) {
        application.scope.launch(Dispatchers.IO) {
            val entry = mediaEntryDao.getEntry(id).first()
            if (entry == null) {
                fetchMediaFlow.emit(id)
            }
        }
    }
}