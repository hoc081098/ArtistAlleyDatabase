package com.thekeeperofpie.artistalleydatabase.art.details

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thekeeperofpie.artistalleydatabase.android_utils.split
import com.thekeeperofpie.artistalleydatabase.android_utils.start
import com.thekeeperofpie.artistalleydatabase.anilist.AniListApi
import com.thekeeperofpie.artistalleydatabase.anilist.character.CharacterRepository
import com.thekeeperofpie.artistalleydatabase.anilist.media.MediaRepository
import com.thekeeperofpie.artistalleydatabase.art.ArtEntry
import com.thekeeperofpie.artistalleydatabase.art.ArtEntryUtils
import com.thekeeperofpie.artistalleydatabase.art.PrintSizeDropdown
import com.thekeeperofpie.artistalleydatabase.art.R
import com.thekeeperofpie.artistalleydatabase.art.SourceDropdown
import com.thekeeperofpie.artistalleydatabase.art.SourceType
import com.thekeeperofpie.artistalleydatabase.art.autocomplete.Autocompleter
import com.thekeeperofpie.artistalleydatabase.art.json.ArtJson
import com.thekeeperofpie.artistalleydatabase.form.EntrySection
import com.thekeeperofpie.artistalleydatabase.form.EntrySection.MultiText.Entry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import java.time.Instant
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
abstract class ArtEntryDetailsViewModel(
    protected val application: Application,
    protected val artEntryDao: ArtEntryDetailsDao,
    private val aniListApi: AniListApi,
    private val mediaRepository: MediaRepository,
    private val characterRepository: CharacterRepository,
    protected val artJson: ArtJson,
    private val autocompleter: Autocompleter,
    protected val dataConverter: ArtEntryDataConverter,
) : ViewModel() {

    protected val seriesSection = EntrySection.MultiText(
        R.string.art_entry_series_header_zero,
        R.string.art_entry_series_header_one,
        R.string.art_entry_series_header_many,
        lockState = EntrySection.LockState.UNLOCKED,
    )

    protected val characterSection = EntrySection.MultiText(
        R.string.art_entry_characters_header_zero,
        R.string.art_entry_characters_header_one,
        R.string.art_entry_characters_header_many,
        lockState = EntrySection.LockState.UNLOCKED,
    )

    protected val sourceSection = SourceDropdown(locked = EntrySection.LockState.UNLOCKED)

    protected val artistSection = EntrySection.MultiText(
        R.string.art_entry_artists_header_zero,
        R.string.art_entry_artists_header_one,
        R.string.art_entry_artists_header_many,
        lockState = EntrySection.LockState.UNLOCKED,
    )
    protected val tagSection = EntrySection.MultiText(
        R.string.art_entry_tags_header_zero,
        R.string.art_entry_tags_header_one,
        R.string.art_entry_tags_header_many,
        lockState = EntrySection.LockState.UNLOCKED,
    )

    protected val printSizeSection = PrintSizeDropdown(lockState = EntrySection.LockState.UNLOCKED)

    protected val notesSection = EntrySection.LongText(
        headerRes = R.string.art_entry_notes_header,
        lockState = EntrySection.LockState.UNLOCKED
    )

    val sections = listOf(
        seriesSection,
        characterSection,
        sourceSection,
        artistSection,
        tagSection,
        printSizeSection,
        notesSection,
    )

    var errorResource by mutableStateOf<Pair<Int, Exception?>?>(null)

    fun onImageSizeResult(width: Int, height: Int) {
        printSizeSection.onSizeChange(width, height)
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            subscribeMultiTextSection(
                section = artistSection,
                localCall = {
                    artEntryDao.queryArtists(it)
                        .map(Entry::Custom)
                        .map { flowOf(it) }
                        .ifEmpty { listOf(flowOf(null)) }
                })
        }
        viewModelScope.launch(Dispatchers.IO) {
            subscribeMultiTextSection(
                seriesSection,
                localCall = autocompleter::querySeriesLocal,
                networkCall = autocompleter::querySeriesNetwork
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                seriesSection.contentUpdates()
                    .map { it.filterIsInstance<Entry.Prefilled>() }
                    .map { it.map { it.id } }
                    .distinctUntilChanged()
                    .flatMapLatest {
                        it.mapNotNull(String::toIntOrNull)
                            .map {
                                aniListApi.charactersByMedia(it)
                                    .map { it.map { dataConverter.characterEntry((it)) } }
                                    .catch {}
                                    .start(emptyList())
                            }
                            .let {
                                combine(it) {
                                    it.fold(mutableListOf<Entry>()) { list, value ->
                                        list.apply { addAll(value) }
                                    }
                                }
                            }
                    }
                    .start(emptyList()),
                characterSection.valueUpdates()
                    .flatMapLatest { query ->
                        autocompleter.queryCharacters(query)
                            .map { query to it }
                    }
            ) { series, (query, charactersPair) ->
                val (charactersFirst, charactersSecond) = charactersPair
                val (seriesFirst, seriesSecond) = series.toMutableList().apply {
                    removeAll { seriesCharacter ->
                        charactersFirst.any { character ->
                            val seriesCharacterEntry = seriesCharacter as? Entry.Prefilled
                            if (seriesCharacterEntry != null) {
                                seriesCharacterEntry.id == (character as? Entry.Prefilled)?.id
                            } else {
                                false
                            }
                        } || charactersSecond.any { character ->
                            val seriesCharacterEntry = seriesCharacter as? Entry.Prefilled
                            if (seriesCharacterEntry != null) {
                                seriesCharacterEntry.id == (character as? Entry.Prefilled)?.id
                            } else {
                                false
                            }
                        }
                    }
                }
                    .split { it.text.contains(query) }
                charactersFirst + seriesFirst + charactersSecond + seriesSecond
            }
                .collectLatest {
                    withContext(Dispatchers.Main) {
                        characterSection.predictions = it.toMutableList()
                    }
                }
        }
        viewModelScope.launch(Dispatchers.IO) {
            subscribeMultiTextSection(
                section = tagSection,
                localCall = {
                    artEntryDao.queryTags(it)
                        .map(Entry::Custom)
                        .map { flowOf(it) }
                        .ifEmpty { listOf(flowOf(null)) }
                })
        }

        viewModelScope.launch(Dispatchers.IO) {
            combine(
                artistSection.contentUpdates(),
                sourceSection.conventionSectionItem.updates(),
                sourceSection.lockStateFlow,
                ::Triple
            )
                .flatMapLatest {
                    // flatMapLatest to immediately drop request if lockState has changed
                    flowOf(it)
                        .filter { (_, _, lockState) ->
                            when (lockState) {
                                EntrySection.LockState.LOCKED -> false
                                EntrySection.LockState.UNLOCKED,
                                EntrySection.LockState.DIFFERENT,
                                null -> true
                            }
                        }
                        .filter { (_, convention, _) ->
                            convention.name.isNotEmpty()
                                    && convention.year != null && convention.year > 1000
                                    && convention.hall.isEmpty() && convention.booth.isEmpty()
                        }
                        .mapNotNull { (artistEntries, convention) ->
                            artistEntries.firstNotNullOfOrNull {
                                artEntryDao
                                    .queryArtistForHallBooth(
                                        it.searchableValue,
                                        convention.name,
                                        convention.year!!
                                    )
                                    .takeUnless { it.isNullOrBlank() }
                                    ?.let<String, SourceType.Convention>(
                                        artJson.json::decodeFromString
                                    )
                                    ?.takeIf {
                                        it.name == convention.name && it.year == convention.year
                                    }
                            }
                        }
                }
                .collectLatest {
                    withContext(Dispatchers.Main) {
                        sourceSection.conventionSectionItem.updateHallBoothIfEmpty(
                            expectedName = it.name,
                            expectedYear = it.year!!,
                            newHall = it.hall,
                            newBooth = it.booth
                        )
                    }
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun subscribeMultiTextSection(
        section: EntrySection.MultiText,
        localCall: suspend (String) -> List<Flow<Entry?>>,
        networkCall: suspend (query: String) -> Flow<List<Entry>> = {
            flowOf(emptyList())
        },
    ) {
        section.valueUpdates()
            .flatMapLatest { query ->
                val database = combine(localCall(query)) { it.toList() }
                val aniList = if (query.isBlank()) flowOf(emptyList()) else networkCall(query)
                combine(database, aniList) { local, network ->
                    local.filterNotNull().toMutableList().apply {
                        removeIf { source ->
                            network.any { target -> target.text.trim() == source.text.trim() }
                        }
                    } + network
                }
            }
            .collectLatest {
                withContext(Dispatchers.Main) {
                    section.predictions = it.toMutableList()
                }
            }
    }

    protected fun buildModel(entry: ArtEntry): ArtEntryModel {
        val artists = entry.artists.map(Entry::Custom)
        val series = entry.series.map(dataConverter::databaseToSeriesEntry)
        val characters = entry.characters.map(dataConverter::databaseToCharacterEntry)
        val tags = entry.tags.map(Entry::Custom)

        return ArtEntryModel(
            entry = entry,
            artists = artists,
            series = series,
            characters = characters,
            tags = tags,
            source = SourceType.fromEntry(artJson.json, entry)
        )
    }

    protected fun initializeForm(entry: ArtEntryModel) {
        artistSection.setContents(entry.artists)
        artistSection.lockState = entry.artistsLocked

        sourceSection.initialize(entry)
        sourceSection.lockState = entry.sourceLocked

        seriesSection.setContents(entry.series)
        seriesSection.lockState = entry.seriesLocked

        characterSection.setContents(entry.characters)
        characterSection.lockState = entry.charactersLocked

        printSizeSection.initialize(entry.printWidth, entry.printHeight)
        printSizeSection.lockState = entry.printSizeLocked

        tagSection.setContents(entry.tags)
        tagSection.lockState = entry.tagsLocked

        notesSection.value = entry.notes.orEmpty()
        notesSection.lockState = entry.notesLocked

        entry.characters.filterIsInstance<Entry.Prefilled>()
            .forEach {
                val characterId = it.id.toInt()
                viewModelScope.launch(Dispatchers.Main) {
                    characterRepository.getEntry(characterId)
                        .filterNotNull()
                        .flatMapLatest { character ->
                            // TODO: Batch query?
                            character.mediaIds
                                ?.map { mediaRepository.getEntry(it) }
                                ?.let { combine(it) { it.toList() } }
                                .let { it ?: flowOf(listOf(null)) }
                                .map { character to it.filterNotNull() }
                        }
                        .map { dataConverter.characterEntry(it.first, it.second) }
                        .filterNotNull()
                        .flowOn(Dispatchers.IO)
                        .collectLatest { newEntry ->
                            characterSection.replaceContents { entry ->
                                if (entry is Entry.Prefilled &&
                                    entry.id == characterId.toString()
                                ) newEntry else entry
                            }
                        }
                }
            }

        entry.series.filterIsInstance<Entry.Prefilled>()
            .forEach {
                val mediaId = it.id.toInt()
                viewModelScope.launch(Dispatchers.Main) {
                    mediaRepository.getEntry(mediaId)
                        .filterNotNull()
                        .map(dataConverter::seriesEntry)
                        .flowOn(Dispatchers.IO)
                        .collectLatest { newEntry ->
                            seriesSection.replaceContents { entry ->
                                if (entry is Entry.Prefilled &&
                                    entry.id == mediaId.toString()
                                ) newEntry else entry
                            }
                        }
                }
            }
    }

    protected suspend fun makeEntry(imageUri: Uri?, id: String): ArtEntry? {
        val outputFile = ArtEntryUtils.getImageFile(application, id)
        val error = ArtEntryUtils.writeEntryImage(application, outputFile, imageUri)
        if (error != null) {
            withContext(Dispatchers.Main) {
                errorResource = error
            }
            return null
        }
        val (imageWidth, imageHeight) = ArtEntryUtils.getImageSize(outputFile)
        val sourceItem = sourceSection.selectedItem().toSource()

        return ArtEntry(
            id = id,
            artists = artistSection.finalContents().map { it.serializedValue },
            sourceType = sourceItem.serializedType,
            sourceValue = sourceItem.serializedValue(artJson.json),
            series = seriesSection.finalContents().map { it.serializedValue },
            seriesSearchable = seriesSection.finalContents().map { it.searchableValue }
                .filterNot(String?::isNullOrBlank),
            characters = characterSection.finalContents().map { it.serializedValue },
            charactersSearchable = characterSection.finalContents().map { it.searchableValue }
                .filterNot(String?::isNullOrBlank),
            tags = tagSection.finalContents().map { it.serializedValue },
            lastEditTime = Date.from(Instant.now()),
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            printWidth = printSizeSection.finalWidth(),
            printHeight = printSizeSection.finalHeight(),
            notes = notesSection.value.trim(),
            locks = ArtEntry.Locks(
                artistsLocked = artistSection.lockState?.toSerializedValue(),
                seriesLocked = seriesSection.lockState?.toSerializedValue(),
                charactersLocked = characterSection.lockState?.toSerializedValue(),
                sourceLocked = sourceSection.lockState?.toSerializedValue(),
                tagsLocked = tagSection.lockState?.toSerializedValue(),
                notesLocked = notesSection.lockState?.toSerializedValue(),
                printSizeLocked = printSizeSection.lockState?.toSerializedValue(),
            )
        )
    }

    suspend fun saveEntry(imageUri: Uri?, id: String) {
        val entry = makeEntry(imageUri, id) ?: return
        entry.series
            .map { artJson.parseSeriesColumn(it) }
            .mapNotNull { it.rightOrNull()?.id }
            .forEach(mediaRepository::ensureSaved)
        entry.characters
            .map { artJson.parseCharacterColumn(it) }
            .mapNotNull { it.rightOrNull()?.id }
            .forEach(characterRepository::ensureSaved)
        artEntryDao.insertEntries(entry)
    }
}