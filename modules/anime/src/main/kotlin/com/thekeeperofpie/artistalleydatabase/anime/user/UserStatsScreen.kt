package com.thekeeperofpie.artistalleydatabase.anime.user

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anilist.UserByIdQuery
import com.anilist.fragment.UserMediaStatistics
import com.anilist.type.MediaFormat
import com.anilist.type.MediaListStatus
import com.thekeeperofpie.artistalleydatabase.anime.R
import com.thekeeperofpie.artistalleydatabase.anime.media.MediaUtils.toColor
import com.thekeeperofpie.artistalleydatabase.anime.media.MediaUtils.toTextRes
import com.thekeeperofpie.artistalleydatabase.anime.ui.DetailsSectionHeader
import com.thekeeperofpie.artistalleydatabase.compose.PieChart
import com.thekeeperofpie.artistalleydatabase.compose.VerticalDivider
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

@Suppress("NAME_SHADOWING")
object UserStatsScreen {

    @Composable
    operator fun invoke(
        user: () -> UserByIdQuery.Data.User?,
        statistics: @Composable () -> UserMediaStatistics?,
        modifier: Modifier = Modifier,
        isAnime: Boolean,
        bottomNavBarPadding: @Composable () -> Dp = { 0.dp },
    ) {
        val statistics = statistics()
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp + bottomNavBarPadding()),
            modifier = modifier.fillMaxSize()
        ) {
            if (statistics == null) {
                item {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp)
                        )
                    }
                }
                return@LazyColumn
            }

            val user = user()
            if (user != null) {
                if (isAnime) {
                    animeStatisticsSection(user)
                } else {
                    mangaStatisticsSection(user)
                }
            }

            formatsSection(statistics)
            statusesSection(statistics, isAnime = isAnime)
        }
    }

    private fun LazyListScope.formatsSection(statistics: UserMediaStatistics) {
        pieChartSection(
            titleRes = R.string.anime_user_stats_formats_label,
            slices = statistics.formats,
            sliceToKey = { it.format },
            sliceToAmount = { it.count },
            sliceToColor = { it.format.toColor() },
            sliceToText = { stringResource(it.format.toTextRes()) },
            keySave = { it?.rawValue.orEmpty() },
            keyRestore = { key ->
                MediaFormat.values().find { it.rawValue == key }
                    ?: MediaFormat.UNKNOWN__
            },
        )
    }

    private fun LazyListScope.statusesSection(statistics: UserMediaStatistics, isAnime: Boolean) {
        pieChartSection(
            titleRes = R.string.anime_user_stats_statuses_label,
            slices = statistics.statuses,
            sliceToKey = { it.status },
            sliceToAmount = { it.count },
            sliceToColor = { it.status.toColor() },
            sliceToText = { stringResource(it.status.toTextRes(anime = isAnime)) },
            keySave = { it?.rawValue.orEmpty() },
            keyRestore = { key ->
                MediaListStatus.values().find { it.rawValue == key }
                    ?: MediaListStatus.UNKNOWN__
            },
        )
    }

    private fun <Key, Value> LazyListScope.pieChartSection(
        @StringRes titleRes: Int,
        slices: List<Value?>?,
        sliceToKey: (Value) -> Key,
        sliceToAmount: (Value) -> Int,
        sliceToColor: (Value) -> Color,
        sliceToText: @Composable (Value) -> String,
        keySave: (Key) -> String,
        keyRestore: (String) -> Key,
    ) {
        val slices = slices?.filterNotNull()?.ifEmpty { null } ?: return
        item {
            DetailsSectionHeader(stringResource(titleRes))
        }

        item {
            ElevatedCard(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            ) {
                PieChart(
                    slices = slices,
                    sliceToKey = sliceToKey,
                    sliceToAmount = sliceToAmount,
                    sliceToColor = sliceToColor,
                    sliceToText = sliceToText,
                    keySave = keySave,
                    keyRestore = keyRestore,
                    pieMaxHeight = 180.dp,
                )
            }
        }
    }

    private fun LazyListScope.animeStatisticsSection(user: UserByIdQuery.Data.User) {
        val statistics = user.statistics?.anime ?: return
        statisticsSection(
            statistics.count.toString() to R.string.anime_user_statistics_count,
            String.format("%.1f", statistics.minutesWatched.minutes.toDouble(DurationUnit.DAYS)) to
                    R.string.anime_user_statistics_anime_days_watched,
            String.format("%.1f", statistics.meanScore) to
                    R.string.anime_user_statistics_mean_score,
        )
    }

    private fun LazyListScope.mangaStatisticsSection(user: UserByIdQuery.Data.User) {
        val statistics = user.statistics?.manga ?: return
        statisticsSection(
            statistics.count.toString() to R.string.anime_user_statistics_count,
            statistics.chaptersRead.toString() to
                    R.string.anime_user_statistics_manga_chapters_read,
            String.format("%.1f", statistics.meanScore) to
                    R.string.anime_user_statistics_mean_score,
        )
    }

    private fun LazyListScope.statisticsSection(
        vararg pairs: Pair<String, Int>,
    ) {
        item {
            ElevatedCard(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    pairs.forEachIndexed { index, pair ->
                        if (index != 0) {
                            VerticalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(0.33f)
                                .padding(8.dp)
                        ) {
                            Text(text = pair.first, color = MaterialTheme.colorScheme.surfaceTint)
                            Text(text = stringResource(pair.second))
                        }
                    }
                }
            }
        }
    }
}
