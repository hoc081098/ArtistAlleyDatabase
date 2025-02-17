package com.thekeeperofpie.artistalleydatabase.anime.media.filter

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anilist.MediaTagsQuery
import com.anilist.fragment.AniListListRowMedia
import com.anilist.type.MediaFormat
import com.anilist.type.MediaListStatus
import com.anilist.type.MediaSeason
import com.anilist.type.MediaSource
import com.anilist.type.MediaStatus
import com.thekeeperofpie.artistalleydatabase.android_utils.kotlin.CustomDispatchers
import com.thekeeperofpie.artistalleydatabase.android_utils.kotlin.combine
import com.thekeeperofpie.artistalleydatabase.android_utils.kotlin.mapLatestNotNull
import com.thekeeperofpie.artistalleydatabase.anilist.oauth.AuthedAniListApi
import com.thekeeperofpie.artistalleydatabase.anime.AnimeSettings
import com.thekeeperofpie.artistalleydatabase.anime.R
import com.thekeeperofpie.artistalleydatabase.anime.ignore.AnimeMediaIgnoreList
import com.thekeeperofpie.artistalleydatabase.anime.list.MediaListSortOption
import com.thekeeperofpie.artistalleydatabase.anime.media.AnimeMediaListRow
import com.thekeeperofpie.artistalleydatabase.anime.media.MediaUtils
import com.thekeeperofpie.artistalleydatabase.anime.media.MediaUtils.toTextRes
import com.thekeeperofpie.artistalleydatabase.compose.filter.FilterEntry
import com.thekeeperofpie.artistalleydatabase.compose.filter.FilterIncludeExcludeState
import com.thekeeperofpie.artistalleydatabase.compose.filter.SortEntry
import com.thekeeperofpie.artistalleydatabase.compose.filter.SortOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.SortedMap
import kotlin.reflect.KClass

@OptIn(ExperimentalCoroutinesApi::class)
class AnimeMediaFilterController<T>(
    sortEnumClass: KClass<T>,
    private val aniListApi: AuthedAniListApi,
    private val settings: AnimeSettings,
    private val ignoreList: AnimeMediaIgnoreList,
    defaultEnabled: List<T> = emptyList(),
) where T : SortOption, T : Enum<*> {

    companion object {
        private const val TAG = "AnimeMediaFilterController"
    }

    val sortOptions = MutableStateFlow(SortEntry.options(sortEnumClass).map {
        if (defaultEnabled.contains(it.value)) it.copy(state = FilterIncludeExcludeState.INCLUDE) else it
    })
    val sortAscending = MutableStateFlow(false)

    val genres = MutableStateFlow(emptyList<GenreEntry>())
    private val genresFiltered = genres.flatMapLatest { genres ->
        settings.showAdult.map {
            if (it) {
                genres
            } else {
                // Keep if previously selected (not DEFAULT)
                genres.filterNot { it.state == FilterIncludeExcludeState.DEFAULT && it.isAdult }
            }
        }
    }

    val tagsByCategory = MutableStateFlow(emptyMap<String, TagSection>())
    private val tagsByCategoryFiltered = tagsByCategory.flatMapLatest { tags ->
        settings.showAdult.map { showAdult ->
            if (showAdult) return@map tags
            tags.values.mapNotNull {
                // Keep if previously selected (not DEFAULT)
                it.filter { it.state != FilterIncludeExcludeState.DEFAULT || it.isAdult != true }
            }
                .associateBy { it.name }
                .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        }
    }

    private val tagRank = MutableStateFlow("0")
    val statuses = MutableStateFlow(StatusEntry.statuses())
    val listStatuses = MutableStateFlow(emptyList<ListStatusEntry>())
    val formats = MutableStateFlow(FormatEntry.formats())

    val onListOptions = MutableStateFlow(OnListOption.options())
    val averageScoreRange = MutableStateFlow(RangeData(100, hardMax = true))
    val episodesRange = MutableStateFlow(RangeData(151))
    val sources = MutableStateFlow(SourceEntry.sources())

    private val airingDate = MutableStateFlow(AiringDate.Basic() to AiringDate.Advanced())
    private val airingDateIsAdvanced = MutableStateFlow(false)

    val showAdult get() = settings.showAdult
    val showIgnored get() = settings.showIgnored

    // These are kept separated so that recomposition can happen per-section
    private var sortExpanded by mutableStateOf(false)
    private var statusExpanded by mutableStateOf(false)
    private var listStatusExpanded by mutableStateOf(false)
    private var formatExpanded by mutableStateOf(false)
    private var genresExpanded by mutableStateOf(false)
    private var tagsExpanded by mutableStateOf(false)
    private var airingDateExpanded by mutableStateOf(false)
    private var onListExpanded by mutableStateOf(false)
    private var averageScoreExpanded by mutableStateOf(false)
    private var episodesExpanded by mutableStateOf(false)
    private var sourceExpanded by mutableStateOf(false)

    private var showExpandAll by mutableStateOf(true)

    private lateinit var initialParams: InitialParams

    @OptIn(ExperimentalCoroutinesApi::class)
    fun initialize(
        viewModel: ViewModel,
        refreshUpdates: StateFlow<*>,
        params: InitialParams,
    ) {
        if (::initialParams.isInitialized) return
        initialParams = params

        listStatuses.value = ListStatusEntry.statuses(params.isAnime)
        val filterData = params.filterData
        if (filterData != null) {
            setFilterData(filterData)
        }

        viewModel.viewModelScope.launch(CustomDispatchers.Main) {
            refreshUpdates
                .mapLatestNotNull {
                    withContext(CustomDispatchers.IO) {
                        try {
                            aniListApi.genres()
                                .genreCollection
                                ?.filterNotNull()
                                ?.map(::GenreEntry)
                                ?.let {
                                    setFilterDataForGenres(
                                        map = it,
                                        included = filterData?.genresIncluded ?: emptyList(),
                                        excluded = filterData?.genresExcluded ?: emptyList(),
                                    )
                                }
                        } catch (e: Exception) {
                            Log.d(TAG, "Error loading genres", e)
                            null
                        }
                    }
                }
                .take(1)
                .collectLatest(genres::emit)
        }

        viewModel.viewModelScope.launch(CustomDispatchers.Main) {
            refreshUpdates
                .mapLatestNotNull {
                    withContext(CustomDispatchers.IO) {
                        try {
                            aniListApi.tags().mediaTagCollection
                                ?.filterNotNull()
                                ?.let(::buildTagSections)
                                ?.run {
                                    val tagsIncluded = filterData?.tagsIncluded ?: emptyList()
                                    val tagsExcluded = filterData?.tagsExcluded ?: emptyList()
                                    if (tagsIncluded.isEmpty() && tagsExcluded.isEmpty()
                                        && initialParams.tagId == null
                                    ) {
                                        return@run this
                                    }

                                    toMutableMap().apply {
                                        replaceAll { _, section ->
                                            section.replace {
                                                if (it.id == initialParams.tagId) {
                                                    it.copy(
                                                        state = FilterIncludeExcludeState.INCLUDE,
                                                        clickable = false
                                                    )
                                                } else {
                                                    it.copy(
                                                        state = FilterIncludeExcludeState.toState(
                                                            it.id,
                                                            included = tagsIncluded,
                                                            excluded = tagsExcluded
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                        } catch (e: Exception) {
                            Log.d(TAG, "Error loading tags", e)
                            null
                        }
                    }
                }
                .take(1)
                .collectLatest(tagsByCategory::emit)
        }
    }

    fun airingDate() = combine(airingDate, airingDateIsAdvanced, ::Pair)
        .map { (airingDatePair, advanced) ->
            if (advanced) airingDatePair.second else airingDatePair.first
        }

    fun tagRank() = tagRank.map { it.toIntOrNull()?.coerceIn(0, 100) }

    private fun setFilterData(filterData: FilterData) {
        // TODO: sortOption and sortListOption
        if (filterData.sortOption != null) {
            sortOptions.value = sortOptions.value.toMutableList().apply {
                replaceAll {
                    it.takeUnless { it.value == filterData.sortOption }
                        ?: it.copy(state = FilterIncludeExcludeState.INCLUDE)
                }
            }
        } else if (filterData.sortListOption != null) {
            sortOptions.value = sortOptions.value.toMutableList().apply {
                replaceAll {
                    it.takeUnless { it.value == filterData.sortListOption }
                        ?: it.copy(state = FilterIncludeExcludeState.INCLUDE)
                }
            }
        }
        sortAscending.value = filterData.sortAscending
        tagRank.value = filterData.tagRank?.toString() ?: "0"
        statuses.value = StatusEntry.statuses(
            included = filterData.statusesIncluded,
            excluded = filterData.statusesExcluded,
        )
        listStatuses.value = ListStatusEntry.statuses(
            isAnime = filterData.isAnime ?: true,
            included = filterData.listStatusesIncluded,
            excluded = filterData.listStatusesExcluded,
        )
        formats.value = FormatEntry.formats(
            included = filterData.formatsIncluded,
            excluded = filterData.formatsExcluded
        )
        onListOptions.value = OnListOption.options(filterData.onList)
        averageScoreRange.value = RangeData(
            maxValue = 100,
            hardMax = true,
            startString = filterData.averageScoreMin?.toString() ?: "0",
            endString = filterData.averageScoreMax?.toString() ?: "100",
        )
        episodesRange.value = RangeData(
            maxValue = 151,
            hardMax = false,
            startString = filterData.episodesMin?.toString() ?: "0",
            endString = filterData.episodesMax?.toString() ?: "",
        )
        sources.value = SourceEntry.sources(
            included = filterData.sourcesIncluded,
            excluded = filterData.sourcesExcluded,
        )
        airingDate.value = AiringDate.Basic(
            season = filterData.airingDateSeason,
            seasonYear = filterData.airingDateSeasonYear?.toString().orEmpty(),
        ) to AiringDate.Advanced(
            startDate = filterData.airingDateStart(),
            endDate = filterData.airingDateEnd(),
        )
        airingDateIsAdvanced.value = filterData.airingDateIsAdvanced

        genres.value = setFilterDataForGenres(
            map = genres.value,
            included = filterData.genresIncluded,
            excluded = filterData.genresExcluded,
        )

        tagsByCategory.value = setFilterDataForTags(
            map = tagsByCategory.value,
            included = filterData.tagsIncluded,
            excluded = filterData.tagsExcluded,
        )
    }

    private fun toFilterData(): FilterData {
        val onListOptions = onListOptions.value
        val containsOnList = onListOptions.find { it.value }?.state == FilterIncludeExcludeState.INCLUDE
        val containsNotOnList =
            onListOptions.find { !it.value }?.state == FilterIncludeExcludeState.INCLUDE
        val onList = when {
            !containsOnList && !containsNotOnList -> null
            containsOnList && containsNotOnList -> null
            else -> containsOnList
        }
        return FilterData(
            sortOption = sortOptions.value
                .firstOrNull { it.state == FilterIncludeExcludeState.INCLUDE }
                ?.value as? MediaSortOption,
            sortListOption = sortOptions.value
                .firstOrNull { it.state == FilterIncludeExcludeState.INCLUDE }
                ?.value as? MediaListSortOption,
            sortAscending = sortAscending.value,
            tagRank = tagRank.value.toIntOrNull(),
            onList = onList,
            isAnime = initialParams.isAnime,
            averageScoreMin = averageScoreRange.value.startInt,
            averageScoreMax = averageScoreRange.value.endInt,
            episodesMin = episodesRange.value.startInt,
            episodesMax = episodesRange.value.startInt,
            sourcesIncluded = sources.value.filter { it.state == FilterIncludeExcludeState.INCLUDE }
                .map { it.value },
            sourcesExcluded = sources.value.filter { it.state == FilterIncludeExcludeState.EXCLUDE }
                .map { it.value },
            statusesIncluded = statuses.value.filter { it.state == FilterIncludeExcludeState.INCLUDE }
                .map { it.value },
            statusesExcluded = statuses.value.filter { it.state == FilterIncludeExcludeState.EXCLUDE }
                .map { it.value },
            listStatusesIncluded = listStatuses.value.filter { it.state == FilterIncludeExcludeState.INCLUDE }
                .map { it.value },
            listStatusesExcluded = listStatuses.value.filter { it.state == FilterIncludeExcludeState.EXCLUDE }
                .map { it.value },
            formatsIncluded = formats.value.filter { it.state == FilterIncludeExcludeState.INCLUDE }
                .map { it.value },
            formatsExcluded = formats.value.filter { it.state == FilterIncludeExcludeState.EXCLUDE }
                .map { it.value },
            airingDateIsAdvanced = airingDateIsAdvanced.value,
            airingDateSeason = airingDate.value.first.season,
            airingDateSeasonYear = airingDate.value.first.seasonYear.toIntOrNull(),
            airingDateStartYear = airingDate.value.second.startDate?.year,
            airingDateStartMonth = airingDate.value.second.startDate?.monthValue,
            airingDateStartDayOfMonth = airingDate.value.second.startDate?.dayOfMonth,
            airingDateEndYear = airingDate.value.second.endDate?.year,
            airingDateEndMonth = airingDate.value.second.endDate?.monthValue,
            airingDateEndDayOfMonth = airingDate.value.second.endDate?.dayOfMonth,
            genresIncluded = genres.value.filter { it.state == FilterIncludeExcludeState.INCLUDE }
                .map { it.value },
            genresExcluded = genres.value.filter { it.state == FilterIncludeExcludeState.EXCLUDE }
                .map { it.value },
            tagsIncluded = tagsByCategory.value.let {
                it.values
                    .flatMap {
                        when (it) {
                            is TagSection.Category -> it.flatten()
                            is TagSection.Tag -> listOf(it)
                        }
                    }
                    .filter { it.state == FilterIncludeExcludeState.INCLUDE }
                    .map { it.id }
            },
            tagsExcluded = tagsByCategory.value.let {
                it.values
                    .flatMap {
                        when (it) {
                            is TagSection.Category -> it.flatten()
                            is TagSection.Tag -> listOf(it)
                        }
                    }
                    .filter { it.state == FilterIncludeExcludeState.EXCLUDE }
                    .map { it.id }
            },
        )
    }

    private fun setFilterDataForGenres(
        map: List<GenreEntry>,
        included: List<String>,
        excluded: List<String>,
    ) = map.toMutableList().apply {
        replaceAll {
            it.copy(
                state = FilterIncludeExcludeState.toState(
                    it.value,
                    included = included,
                    excluded = excluded,
                )
            )
        }
    }

    private fun setFilterDataForTags(
        map: Map<String, TagSection>,
        included: List<String>,
        excluded: List<String>,
    ) = map.toMutableMap().apply {
        replaceAll { _, section ->
            section.replace {
                if (it.id == initialParams.tagId) {
                    it.copy(
                        state = FilterIncludeExcludeState.INCLUDE,
                        clickable = false
                    )
                } else {
                    it.copy(
                        state = FilterIncludeExcludeState.toState(
                            it.id,
                            included = included,
                            excluded = excluded,
                        )
                    )
                }
            }
        }
    }

    private fun getExpanded(section: Section) = when (section) {
        Section.SORT -> sortExpanded
        Section.STATUS -> statusExpanded
        Section.LIST_STATUS -> listStatusExpanded
        Section.FORMAT -> formatExpanded
        Section.GENRES -> genresExpanded
        Section.TAGS -> tagsExpanded
        Section.AIRING_DATE -> airingDateExpanded
        Section.ON_LIST -> onListExpanded
        Section.AVERAGE_SCORE -> averageScoreExpanded
        Section.EPISODES -> episodesExpanded
        Section.SOURCE -> sourceExpanded
    }

    private fun setExpanded(section: Section, expanded: Boolean) = when (section) {
        Section.SORT -> sortExpanded = expanded
        Section.STATUS -> statusExpanded = expanded
        Section.LIST_STATUS -> listStatusExpanded = expanded
        Section.FORMAT -> formatExpanded = expanded
        Section.GENRES -> genresExpanded = expanded
        Section.TAGS -> tagsExpanded = expanded
        Section.AIRING_DATE -> airingDateExpanded = expanded
        Section.ON_LIST -> onListExpanded = expanded
        Section.AVERAGE_SCORE -> averageScoreExpanded = expanded
        Section.EPISODES -> episodesExpanded = expanded
        Section.SOURCE -> sourceExpanded = expanded
    }.also {
        showExpandAll = Section.values().none(::getExpanded)
    }

    /**
     * Categories are provided from the API in the form of "Parent-Child". This un-flattens the
     * tag list into a tree of sections, separated by the "-" dash.
     */
    private fun buildTagSections(tags: List<MediaTagsQuery.Data.MediaTagCollection>): Map<String, TagSection> {
        val sections = mutableMapOf<String, Any>()
        tags.forEach {
            var categories = it.category?.split('-')

            // Manually handle the "Sci-Fi" category, which contains a dash, but shouldn't be split
            if (categories != null) {
                val sciIndex = categories.indexOf("Sci")
                if (sciIndex >= 0) {
                    val hasFi = categories.getOrNull(sciIndex + 1) == "Fi"
                    if (hasFi) {
                        categories = categories.toMutableList().apply {
                            removeAt(sciIndex + 1)
                            set(sciIndex, "Sci-Fi")
                        }
                    }
                }
            }

            var currentCategory: TagSection.Category.Builder? = null
            categories?.forEach {
                currentCategory = if (currentCategory == null) {
                    sections.getOrPut(it) { TagSection.Category.Builder(it) }
                            as TagSection.Category.Builder
                } else {
                    (currentCategory as TagSection.Category.Builder).getOrPutCategory(it)
                }
            }

            if (currentCategory == null) {
                sections[it.name] = TagSection.Tag(it)
            } else {
                currentCategory!!.addChild(it)
            }
        }

        return sections.mapValues { (_, value) ->
            when (value) {
                is TagSection.Category.Builder -> value.build()
                is TagSection.Tag -> value
                else -> throw IllegalStateException("Unexpected value $value")
            }
        }
    }

    private fun onSortClick(option: T) {
        sortOptions.value = sortOptions.value.toMutableList()
            .apply {
                replaceAll {
                    if (it.value == option) {
                        val newState = if (it.state != FilterIncludeExcludeState.INCLUDE) {
                            FilterIncludeExcludeState.INCLUDE
                        } else {
                            FilterIncludeExcludeState.DEFAULT
                        }
                        it.copy(state = newState)
                    } else it.copy(state = FilterIncludeExcludeState.DEFAULT)
                }
            }
    }

    private fun onSortAscendingChange(ascending: Boolean) = sortAscending.update { ascending }

    private fun onGenreClick(genreName: String) {
        genres.value = genres.value.toMutableList()
            .apply {
                replaceAll {
                    if (it.value == genreName) {
                        it.copy(state = it.state.next())
                    } else it
                }
            }
    }

    private fun onTagClick(tagId: String) {
        if (tagId == initialParams.tagId) return
        tagsByCategory.value = tagsByCategory.value
            .mapValues { (_, value) ->
                value.replace {
                    it.takeUnless { it.id == tagId }
                        ?: it.copy(state = it.state.next())
                }
            }
    }

    private fun onStatusClick(status: MediaStatus) {
        statuses.value = statuses.value.toMutableList()
            .apply {
                replaceAll {
                    if (it.value == status) {
                        it.copy(state = it.state.next())
                    } else it
                }
            }
    }

    private fun onListStatusClick(status: MediaListStatus) {
        listStatuses.value = listStatuses.value.toMutableList()
            .apply {
                replaceAll {
                    if (it.value == status) {
                        if (initialParams.showListStatusExcludes) {
                            it.copy(state = it.state.next())
                        } else {
                            val newState = if (it.state != FilterIncludeExcludeState.INCLUDE) {
                                FilterIncludeExcludeState.INCLUDE
                            } else {
                                FilterIncludeExcludeState.DEFAULT
                            }
                            it.copy(state = newState)
                        }
                    } else it
                }
            }
    }

    private fun onFormatClick(format: MediaFormat) {
        formats.value = formats.value.toMutableList()
            .apply {
                replaceAll {
                    if (it.value == format) {
                        it.copy(state = it.state.next())
                    } else it
                }
            }
    }

    private fun onSeasonChange(season: MediaSeason?) {
        val value = airingDate.value
        airingDate.value = value.copy(first = value.first.copy(season = season))
    }

    private fun onSeasonYearChange(seasonYear: String) {
        val value = airingDate.value
        airingDate.value = value.copy(first = value.first.copy(seasonYear = seasonYear))
    }

    private fun onAiringDateChange(start: Boolean, selectedMillis: Long?) {
        // Selected value is in UTC
        val selectedDate = selectedMillis?.let {
            Instant.ofEpochMilli(it)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
        }

        val value = airingDate.value
        airingDate.value = value.copy(
            second = if (start) {
                value.second.copy(startDate = selectedDate)
            } else {
                value.second.copy(endDate = selectedDate)
            }
        )
    }

    private fun onOnListClick(option: OnListOption) {
        onListOptions.value = onListOptions.value.toMutableList()
            .apply {
                replaceAll {
                    if (it.value == option.value) {
                        val newState = if (it.state != FilterIncludeExcludeState.INCLUDE) {
                            FilterIncludeExcludeState.INCLUDE
                        } else {
                            FilterIncludeExcludeState.DEFAULT
                        }
                        it.copy(state = newState)
                    } else it.copy(state = FilterIncludeExcludeState.DEFAULT)
                }
            }
    }

    private fun onAverageScoreChange(start: String, end: String) {
        averageScoreRange.value = averageScoreRange.value.copy(startString = start, endString = end)
    }

    private fun onEpisodesChange(start: String, end: String) {
        episodesRange.value = episodesRange.value.copy(
            startString = start,
            endString = end.takeIf { it != "150" }.orEmpty(),
        )
    }

    private fun onSourceClick(source: MediaSource) {
        sources.value = sources.value.toMutableList()
            .apply {
                replaceAll {
                    if (it.value == source) {
                        val newState = if (it.state != FilterIncludeExcludeState.INCLUDE) {
                            FilterIncludeExcludeState.INCLUDE
                        } else {
                            FilterIncludeExcludeState.DEFAULT
                        }
                        it.copy(state = newState)
                    } else it
                }
            }
    }

    private fun onClickExpandAll(expand: Boolean) {
        Section.values().forEach { setExpanded(it, expand) }
        showExpandAll = !expand
    }

    private fun onClearFilter() {
        setFilterData(FilterData())
        sortOptions.value = sortOptions.value.toMutableList().apply {
            replaceAll {
                it.takeIf { it.state == FilterIncludeExcludeState.DEFAULT }
                    ?: it.copy(state = FilterIncludeExcludeState.DEFAULT)
            }
        }
    }

    private fun onLoadFilter() {
        settings.savedAnimeFilters.value.values.firstOrNull()?.let(::setFilterData)
    }

    private fun onSaveFilter() {
        settings.savedAnimeFilters.value = settings.savedAnimeFilters.value.toMutableMap().apply {
            put("Default", toFilterData())
        }
    }

    fun data() = Data(
        expanded = ::getExpanded,
        setExpanded = ::setExpanded,
        sortOptions = { sortOptions.collectAsState().value },
        onSortClick = ::onSortClick,
        sortAscending = { sortAscending.collectAsState().value },
        onSortAscendingChange = ::onSortAscendingChange,
        statuses = { statuses.collectAsState().value },
        onStatusClick = ::onStatusClick,
        listStatuses = { listStatuses.collectAsState().value },
        onListStatusClick = ::onListStatusClick,
        formats = { formats.collectAsState().value },
        onFormatClick = ::onFormatClick,
        genres = { genresFiltered.collectAsState(emptyList()).value },
        onGenreClick = ::onGenreClick,
        tags = { tagsByCategoryFiltered.collectAsState(emptyMap()).value },
        onTagClick = ::onTagClick,
        tagRank = { tagRank.collectAsState().value },
        onTagRankChange = { tagRank.value = it },
        airingDate = {
            airingDate.collectAsState().value.let {
                if (airingDateIsAdvanced.collectAsState().value) it.second else it.first
            }
        },
        onAiringDateIsAdvancedToggle = { airingDateIsAdvanced.value = it },
        onAiringDateChange = ::onAiringDateChange,
        onSeasonChange = ::onSeasonChange,
        onSeasonYearChange = ::onSeasonYearChange,
        onListEnabled = { initialParams.onListEnabled },
        onListOptions = { onListOptions.collectAsState().value },
        onOnListClick = ::onOnListClick,
        averageScoreRange = { averageScoreRange.collectAsState().value },
        onAverageScoreChange = ::onAverageScoreChange,
        episodesRange = { episodesRange.collectAsState().value },
        onEpisodesChange = ::onEpisodesChange,
        sources = { sources.collectAsState().value },
        onSourceClick = ::onSourceClick,
        showAdult = { settings.showAdult.collectAsState().value },
        onShowAdultToggle = { settings.showAdult.value = it },
        showExpandAll = { showExpandAll },
        onClickExpandAll = ::onClickExpandAll,
        collapseOnClose = { settings.collapseAnimeFiltersOnClose.collectAsState().value },
        onCollapseOnCloseToggle = { settings.collapseAnimeFiltersOnClose.value = it },
        showIgnored = { settings.showIgnored.collectAsState().value },
        onShowIgnoredToggle = { settings.showIgnored.value = it },
        onClearFilter = ::onClearFilter,
        onLoadFilter = ::onLoadFilter,
        onSaveFilter = ::onSaveFilter,
    )

    fun filterParams() = combine(
        genres,
        tagsByCategory,
        tagRank(),
        statuses,
        listStatuses,
        formats,
        averageScoreRange,
        episodesRange,
        onListOptions,
        showAdult,
        showIgnored,
        airingDate(),
        sources,
        ::FilterParams
    )

    fun <MediaEntryType : AnimeMediaListRow.MediaEntry<MediaType>, MediaType : AniListListRowMedia> filterEntries(
        filterParams: FilterParams,
        entries: List<MediaEntryType>,
        forceShowIgnored: Boolean = false,
    ): List<MediaEntryType> {
        var filteredEntries = entries

        filteredEntries = FilterIncludeExcludeState.applyFiltering(
            filterParams.statuses,
            filteredEntries,
            state = { it.state },
            key = { it.value },
            transform = { listOfNotNull(it.media.status) }
        )

        filteredEntries = FilterIncludeExcludeState.applyFiltering(
            filterParams.formats,
            filteredEntries,
            state = { it.state },
            key = { it.value },
            transform = { listOfNotNull(it.media.format) }
        )

        filteredEntries = FilterIncludeExcludeState.applyFiltering(
            filterParams.genres,
            filteredEntries,
            state = { it.state },
            key = { it.value },
            transform = { it.media.genres?.filterNotNull().orEmpty() }
        )

        val tagRank = filterParams.tagRank
        val transformIncludes: ((AnimeMediaListRow.MediaEntry<*>) -> List<String>)? =
            if (tagRank == null) null else {
                {
                    it.media.tags
                        ?.filterNotNull()
                        ?.filter { it.rank?.let { it >= tagRank } == true }
                        ?.map { it.id.toString() }
                        .orEmpty()
                }
            }

        filteredEntries = FilterIncludeExcludeState.applyFiltering(
            filterParams.tagsByCategory.values.flatMap {
                when (it) {
                    is TagSection.Category -> it.flatten()
                    is TagSection.Tag -> listOf(it)
                }
            },
            filteredEntries,
            state = { it.state },
            key = { it.value.id.toString() },
            transform = { it.media.tags?.filterNotNull()?.map { it.id.toString() }.orEmpty() },
            transformIncludes = transformIncludes,
        )

        if (!filterParams.showAdult) {
            filteredEntries = filteredEntries.filterNot { it.media.isAdult ?: false }
        }

        if (!filterParams.showIgnored && !forceShowIgnored) {
            filteredEntries = filteredEntries.filterNot { ignoreList.get(it.media.id) }
        }

        filteredEntries = when (val airingDate = filterParams.airingDate) {
            is AiringDate.Basic -> {
                filteredEntries.filter {
                    val season = airingDate.season
                    val seasonYear = airingDate.seasonYear.toIntOrNull()
                    (seasonYear == null || it.media.seasonYear == seasonYear)
                            && (season == null || it.media.season == season)
                }
            }
            is AiringDate.Advanced -> {
                val startDate = airingDate.startDate
                val endDate = airingDate.endDate

                if (startDate == null && endDate == null) {
                    filteredEntries
                } else {
                    fun List<MediaEntryType>.filterStartDate(
                        startDate: LocalDate
                    ) = filter {
                        val mediaStartDate = it.media.startDate
                        val mediaYear = mediaStartDate?.year
                        if (mediaYear == null) {
                            return@filter false
                        } else if (mediaYear > startDate.year) {
                            return@filter true
                        } else if (mediaYear < startDate.year) {
                            return@filter false
                        }

                        val mediaMonth = mediaStartDate.month
                        val mediaDayOfMonth = mediaStartDate.day

                        // TODO: Is this the correct behavior?
                        // If there's no month, match the media to avoid stripping expected result
                        if (mediaMonth == null) {
                            return@filter true
                        }

                        if (mediaMonth < startDate.monthValue) {
                            return@filter false
                        }

                        if (mediaMonth > startDate.monthValue) {
                            return@filter true
                        }

                        mediaDayOfMonth == null || mediaDayOfMonth >= startDate.dayOfMonth
                    }

                    fun List<MediaEntryType>.filterEndDate(
                        endDate: LocalDate
                    ) = filter {
                        val mediaStartDate = it.media.startDate
                        val mediaYear = mediaStartDate?.year
                        if (mediaYear == null) {
                            return@filter false
                        } else if (mediaYear > endDate.year) {
                            return@filter false
                        } else if (mediaYear < endDate.year) {
                            return@filter true
                        }

                        val mediaMonth = mediaStartDate.month
                        val mediaDayOfMonth = mediaStartDate.day

                        // TODO: Is this the correct behavior?
                        // If there's no month, match the media to avoid stripping expected result
                        if (mediaMonth == null) {
                            return@filter true
                        }

                        if (mediaMonth < endDate.monthValue) {
                            return@filter true
                        }

                        if (mediaMonth > endDate.monthValue) {
                            return@filter false
                        }

                        mediaDayOfMonth == null || mediaDayOfMonth <= endDate.dayOfMonth
                    }

                    if (startDate != null && endDate != null) {
                        filteredEntries.filterStartDate(startDate)
                            .filterEndDate(endDate)
                    } else if (startDate != null) {
                        filteredEntries.filterStartDate(startDate)
                    } else if (endDate != null) {
                        filteredEntries.filterEndDate(endDate)
                    } else {
                        filteredEntries
                    }
                }
            }
        }

        val averageScore = filterParams.averageScoreRange
        val averageScoreStart = averageScore.startInt ?: 0
        val averageScoreEnd = averageScore.endInt
        if (averageScoreStart > 0) {
            filteredEntries = filteredEntries.filter {
                it.media.averageScore.let { it != null && it >= averageScoreStart }
            }
        }
        if (averageScoreEnd != null) {
            filteredEntries = filteredEntries.filter {
                it.media.averageScore.let { it != null && it <= averageScoreEnd }
            }
        }

        val episodes = filterParams.episodesRange
        val episodesStart = episodes.startInt ?: 0
        val episodesEnd = episodes.endInt
        if (episodesStart > 0) {
            filteredEntries = filteredEntries.filter {
                it.media.episodes.let { it != null && it >= episodesStart }
            }
        }
        if (episodesEnd != null) {
            filteredEntries = filteredEntries.filter {
                it.media.episodes.let { it != null && it <= episodesEnd }
            }
        }

        filteredEntries = FilterIncludeExcludeState.applyFiltering(
            filterParams.sources,
            filteredEntries,
            state = { it.state },
            key = { it.value },
            transform = { listOfNotNull(it.media.source) }
        )

        return filteredEntries
    }

    data class InitialParams(
        val isAnime: Boolean,
        val onListEnabled: Boolean = true,
        // TODO: Handle tags split by media type
        val tagId: String? = null,
        val filterData: FilterData? = null,
        val showListStatusExcludes: Boolean = false,
    )

    class Data<SortType : SortOption>(
        val expanded: (Section) -> Boolean = { false },
        val setExpanded: (Section, Boolean) -> Unit = { _, _ -> },
        val sortOptions: @Composable () -> List<SortEntry<SortType>>,
        val onSortClick: (SortType) -> Unit = {},
        val sortAscending: @Composable () -> Boolean = { false },
        val onSortAscendingChange: (Boolean) -> Unit = {},
        val statuses: @Composable () -> List<StatusEntry> = { emptyList() },
        val onStatusClick: (MediaStatus) -> Unit = {},
        val listStatuses: @Composable () -> List<ListStatusEntry> = { emptyList() },
        val onListStatusClick: (MediaListStatus) -> Unit = {},
        val formats: @Composable () -> List<FormatEntry> = { emptyList() },
        val onFormatClick: (MediaFormat) -> Unit = {},
        val genres: @Composable () -> List<GenreEntry> = { emptyList() },
        val onGenreClick: (String) -> Unit = {},
        val tags: @Composable () -> Map<String, TagSection> = { emptyMap() },
        val onTagClick: (String) -> Unit = {},
        val tagRank: @Composable () -> String = { "" },
        val onTagRankChange: (String) -> Unit = {},
        val airingDate: @Composable () -> AiringDate = { AiringDate.Basic() },
        val onSeasonChange: (MediaSeason?) -> Unit = {},
        val onSeasonYearChange: (String) -> Unit = {},
        val onAiringDateIsAdvancedToggle: (Boolean) -> Unit = {},
        val onAiringDateChange: (start: Boolean, selectedMillis: Long?) -> Unit = { _, _ -> },
        val onListEnabled: () -> Boolean = { true },
        val onListOptions: @Composable () -> List<OnListOption> = { OnListOption.options() },
        val onOnListClick: (OnListOption) -> Unit = {},
        val averageScoreRange: @Composable () -> RangeData = { RangeData(100) },
        val onAverageScoreChange: (start: String, end: String) -> Unit = { _, _ -> },
        val episodesRange: @Composable () -> RangeData = { RangeData(151) },
        val onEpisodesChange: (start: String, end: String) -> Unit = { _, _ -> },
        val sources: @Composable () -> List<SourceEntry> = { emptyList() },
        val onSourceClick: (MediaSource) -> Unit = {},
        val showAdult: @Composable () -> Boolean = { false },
        val onShowAdultToggle: (Boolean) -> Unit = {},
        val showExpandAll: () -> Boolean = { true },
        val onClickExpandAll: (expand: Boolean) -> Unit = {},
        val collapseOnClose: @Composable () -> Boolean = { true },
        val onCollapseOnCloseToggle: (Boolean) -> Unit = {},
        val showIgnored: @Composable () -> Boolean = { true },
        val onShowIgnoredToggle: (Boolean) -> Unit = {},
        val onClearFilter: () -> Unit = {},
        val onLoadFilter: () -> Unit = {},
        val onSaveFilter: () -> Unit = {},
    ) {
        companion object {
            inline fun <reified T> forPreview(): Data<T>
                    where T : SortOption, T : Enum<*> {
                val enumConstants = T::class.java.enumConstants!!.toList()
                return Data(
                    sortOptions = { enumConstants.map(::SortEntry) },
                    genres = {
                        listOf("Action", "Adventure", "Drama", "Fantasy")
                            .map(::GenreEntry)
                    },
                    tags = {
                        mapOf(
                            "CategoryOne" to TagSection.Category(
                                name = "CategoryOne",
                                children = listOf("TagOne", "TagTwo", "TagThree")
                                    .mapIndexed { index, tag ->
                                        MediaTagsQuery.Data.MediaTagCollection(
                                            id = index,
                                            name = tag
                                        )
                                    }
                                    .map(TagSection::Tag)
                                    .associateBy { it.name }
                                    .toSortedMap()
                            ),
                            "Category" to TagSection.Category(
                                name = "Category",
                                children = mapOf(
                                    "Two" to TagSection.Category(
                                        name = "Two",
                                        children = listOf("TagFour", "TagFive", "TagSix")
                                            .mapIndexed { index, tag ->
                                                MediaTagsQuery.Data.MediaTagCollection(
                                                    id = index,
                                                    name = tag
                                                )
                                            }
                                            .map(TagSection::Tag)
                                            .associateBy { it.name }
                                            .toSortedMap())
                                ).toSortedMap(),
                            ),
                        )
                    },
                    statuses = { StatusEntry.statuses() },
                    formats = { FormatEntry.formats() },
                )
            }
        }
    }

    enum class Section {
        SORT,
        STATUS,
        LIST_STATUS,
        FORMAT,
        GENRES,
        TAGS,
        AIRING_DATE,
        ON_LIST,
        AVERAGE_SCORE,
        EPISODES,
        SOURCE,
    }

    data class StatusEntry(
        override val value: MediaStatus,
        override val state: FilterIncludeExcludeState = FilterIncludeExcludeState.DEFAULT,
    ) : FilterEntry<MediaStatus> {
        companion object {
            fun statuses(
                included: List<MediaStatus> = emptyList(),
                excluded: List<MediaStatus> = emptyList()
            ) = listOf(
                MediaStatus.FINISHED,
                MediaStatus.RELEASING,
                MediaStatus.NOT_YET_RELEASED,
                MediaStatus.CANCELLED,
                MediaStatus.HIATUS,
            ).map {
                StatusEntry(
                    it, FilterIncludeExcludeState.toState(
                        value = it,
                        included = included,
                        excluded = excluded
                    )
                )
            }
        }

        val textRes = value.toTextRes()
    }

    data class ListStatusEntry(
        override val value: MediaListStatus,
        override val state: FilterIncludeExcludeState = FilterIncludeExcludeState.DEFAULT,
        val isAnime: Boolean,
    ) : FilterEntry<MediaListStatus> {
        companion object {
            fun statuses(
                isAnime: Boolean,
                included: List<MediaListStatus> = emptyList(),
                excluded: List<MediaListStatus> = emptyList(),
            ) = listOf(
                MediaListStatus.CURRENT,
                MediaListStatus.PLANNING,
                MediaListStatus.COMPLETED,
                MediaListStatus.DROPPED,
                MediaListStatus.PAUSED,
                MediaListStatus.REPEATING,
            ).map {
                ListStatusEntry(
                    it,
                    FilterIncludeExcludeState.toState(
                        value = it,
                        included = included,
                        excluded = excluded
                    ),
                    isAnime,
                )
            }
        }

        val textRes = value.toTextRes(anime = isAnime)
    }

    data class FormatEntry(
        override val value: MediaFormat,
        override val state: FilterIncludeExcludeState = FilterIncludeExcludeState.DEFAULT,
    ) : FilterEntry<MediaFormat> {
        companion object {
            fun formats(
                included: List<MediaFormat> = emptyList(),
                excluded: List<MediaFormat> = emptyList()
            ) = listOf(
                MediaFormat.TV,
                MediaFormat.TV_SHORT,
                MediaFormat.MOVIE,
                MediaFormat.SPECIAL,
                MediaFormat.OVA,
                MediaFormat.ONA,
                MediaFormat.MUSIC,
                // MANGA, NOVEL, and ONE_SHOT excluded since not anime
            ).map {
                FormatEntry(
                    it, FilterIncludeExcludeState.toState(
                        value = it,
                        included = included,
                        excluded = excluded
                    )
                )
            }
        }

        val textRes = value.toTextRes()
    }

    data class GenreEntry(
        override val value: String,
        override val state: FilterIncludeExcludeState = FilterIncludeExcludeState.DEFAULT
    ) : FilterEntry<String> {
        val isAdult = value == "Hentai"

        override val leadingIconVector = MediaUtils.tagLeadingIcon(isAdult = isAdult)

        override val leadingIconContentDescription =
            MediaUtils.tagLeadingIconContentDescription(isAdult = isAdult)
    }

    sealed interface TagSection {
        val name: String

        fun findTag(id: String): Tag? = when (this) {
            is Category -> {
                children.values.asSequence()
                    .mapNotNull { it.findTag(id) }
                    .firstOrNull()
            }
            is Tag -> takeIf { it.id == id }
        }

        fun filter(predicate: (Tag) -> Boolean): TagSection? = when (this) {
            is Category -> {
                children.values
                    .mapNotNull { it.filter(predicate) }
                    .associateBy { it.name }
                    .toSortedMap(String.CASE_INSENSITIVE_ORDER)
                    .takeIf { it.isNotEmpty() }
                    ?.let { copy(children = it) }
            }
            is Tag -> takeIf { predicate(it) }
        }

        fun replace(block: (Tag) -> Tag): TagSection = when (this) {
            is Category -> {
                copy(children = children.mapValues { (_, value) -> value.replace(block) }
                    .toSortedMap(String.CASE_INSENSITIVE_ORDER))
            }
            is Tag -> block(this)
        }

        data class Category(
            override val name: String,
            val children: SortedMap<String, TagSection>,
            val expanded: Boolean = false,
            val hasAnySelected: Boolean = false,
        ) : TagSection {

            fun flatten(): List<Tag> = children.values.flatMap {
                when (it) {
                    is Category -> it.flatten()
                    is Tag -> listOf(it)
                }
            }

            class Builder(private val name: String) {
                private var children = mutableMapOf<String, Any>()

                fun getOrPutCategory(name: String): Builder {
                    // Prefix to ensure tags don't conflict via name with actual child tags
                    val key = "category_$name"
                    return when (val existingSection = children[key]) {
                        is Builder -> existingSection
                        is Tag -> Builder(name)
                            .apply { children[key] = existingSection }
                            .also { children[key] = it }
                        else -> Builder(name)
                            .also { children[key] = it }
                    }
                }

                fun addChild(it: MediaTagsQuery.Data.MediaTagCollection) {
                    children[it.name] = Tag(
                        id = it.id.toString(),
                        name = it.name,
                        isAdult = it.isAdult,
                        description = it.description,
                        value = it,
                    )
                }

                fun build(): Category = Category(
                    name = name,
                    children = children.mapValues { (_, value) ->
                        when (value) {
                            is Builder -> value.build()
                            is Tag -> value
                            else -> throw IllegalStateException("Unexpected value $value")
                        }
                    }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
                )
            }
        }

        data class Tag(
            val id: String,
            override val name: String,
            val description: String?,
            val isAdult: Boolean?,
            override val value: MediaTagsQuery.Data.MediaTagCollection,
            override val state: FilterIncludeExcludeState = FilterIncludeExcludeState.DEFAULT,
            val clickable: Boolean = true,
        ) : FilterEntry<MediaTagsQuery.Data.MediaTagCollection>, TagSection {
            override val leadingIconVector = MediaUtils.tagLeadingIcon(
                isAdult = isAdult,
                isGeneralSpoiler = value.isGeneralSpoiler,
            )

            override val leadingIconContentDescription =
                MediaUtils.tagLeadingIconContentDescription(
                    isAdult = isAdult,
                    isGeneralSpoiler = value.isGeneralSpoiler,
                )

            constructor(tag: MediaTagsQuery.Data.MediaTagCollection) : this(
                id = tag.id.toString(),
                name = tag.name,
                isAdult = tag.isAdult,
                description = tag.description,
                value = tag,
            )
        }
    }

    data class OnListOption(
        override val value: Boolean,
        override val state: FilterIncludeExcludeState = FilterIncludeExcludeState.DEFAULT,
    ) : FilterEntry<Boolean?> {
        companion object {
            fun options(default: Boolean? = null) = listOf(true, false).map {
                OnListOption(
                    it,
                    state = if (it == default) {
                        FilterIncludeExcludeState.INCLUDE
                    } else {
                        FilterIncludeExcludeState.DEFAULT
                    }
                )
            }
        }

        val textRes = when (value) {
            true -> R.string.anime_media_filter_on_list_on_list
            false -> R.string.anime_media_filter_on_list_not_on_list
        }
    }

    sealed interface AiringDate {

        data class Basic(
            val season: MediaSeason? = null,
            val seasonYear: String = "",
        ) : AiringDate

        data class Advanced(
            val startDate: LocalDate? = null,
            val endDate: LocalDate? = null,
        ) : AiringDate
    }

    data class RangeData(
        val maxValue: Int = 100,
        val hardMax: Boolean = false,
        val startString: String = "0",
        val endString: String = if (hardMax) maxValue.toString() else "",
    ) {
        val startInt = startString.toIntOrNull()?.takeIf { it > 0 }?.let {
            if (hardMax) it.coerceAtMost(maxValue) else it
        }
        val endInt = endString.toIntOrNull()?.takeIf { it > 0 }?.let {
            if (hardMax) it.coerceAtMost(maxValue) else it
        }

        val apiStart = startInt?.takeIf { it > 0 }
        val apiEnd = endInt?.takeIf { it != maxValue || !hardMax }?.let { it + 1 }

        val summaryText = if (startInt != null && endInt != null) {
            if (startInt == endInt) {
                startInt.toString()
            } else if (endInt == maxValue && hardMax) {
                "≥ $startInt"
            } else {
                "$startString - $endString"
            }
        } else if (startInt != null) {
            "≥ $startInt"
        } else if (endInt != null) {
            if (hardMax && endInt == maxValue) null else "≤ $endInt"
        } else null

        val value = if (startInt != null && endInt != null) {
            startInt.coerceAtMost(maxValue).toFloat()..endInt.coerceAtMost(maxValue).toFloat()
        } else if (startInt != null) {
            startInt.coerceAtMost(maxValue).toFloat()..maxValue.toFloat()
        } else if (endInt != null) {
            0f..endInt.toFloat()
        } else {
            0f..maxValue.toFloat()
        }

        val valueRange = 0f..maxValue.toFloat()
    }

    data class SourceEntry(
        override val value: MediaSource,
        override val state: FilterIncludeExcludeState = FilterIncludeExcludeState.DEFAULT,
    ) : FilterEntry<MediaSource> {
        companion object {
            fun sources(
                included: List<MediaSource> = emptyList(),
                excluded: List<MediaSource> = emptyList(),
            ) = listOf(
                MediaSource.ORIGINAL,
                MediaSource.ANIME,
                MediaSource.COMIC,
                MediaSource.DOUJINSHI,
                MediaSource.GAME,
                MediaSource.LIGHT_NOVEL,
                MediaSource.LIVE_ACTION,
                MediaSource.MANGA,
                MediaSource.MULTIMEDIA_PROJECT,
                MediaSource.NOVEL,
                MediaSource.OTHER,
                MediaSource.PICTURE_BOOK,
                MediaSource.VIDEO_GAME,
                MediaSource.VISUAL_NOVEL,
                MediaSource.WEB_NOVEL,
            ).map {
                SourceEntry(
                    it, FilterIncludeExcludeState.toState(
                        value = it,
                        included = included,
                        excluded = excluded
                    )
                )
            }
        }

        @StringRes
        val textRes = value.toTextRes()
    }

    data class FilterParams(
        val genres: List<FilterEntry<String>>,
        val tagsByCategory: Map<String, TagSection>,
        val tagRank: Int?,
        val statuses: List<StatusEntry>,
        val listStatuses: List<ListStatusEntry>,
        val formats: List<FormatEntry>,
        val averageScoreRange: RangeData,
        val episodesRange: RangeData,
        val onListOptions: List<OnListOption>,
        val showAdult: Boolean,
        val showIgnored: Boolean,
        val airingDate: AiringDate,
        val sources: List<SourceEntry>,
    )
}
