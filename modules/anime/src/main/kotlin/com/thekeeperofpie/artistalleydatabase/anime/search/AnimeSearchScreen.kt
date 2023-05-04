package com.thekeeperofpie.artistalleydatabase.anime.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.anilist.MediaAdvancedSearchQuery.Data.Page.Medium
import com.thekeeperofpie.artistalleydatabase.anime.R
import com.thekeeperofpie.artistalleydatabase.anime.media.AnimeMediaListRow
import com.thekeeperofpie.artistalleydatabase.anime.media.AnimeMediaListScreen
import com.thekeeperofpie.artistalleydatabase.anime.media.filter.AnimeMediaFilterController
import com.thekeeperofpie.artistalleydatabase.anime.media.filter.AnimeMediaFilterOptionsBottomPanel
import com.thekeeperofpie.artistalleydatabase.anime.media.filter.MediaSortOption
import com.thekeeperofpie.artistalleydatabase.compose.AppBar
import com.thekeeperofpie.artistalleydatabase.compose.NavMenuIconButton
import com.thekeeperofpie.artistalleydatabase.compose.ScrollStateSaver
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
object AnimeSearchScreen {

    @Composable
    operator fun invoke(
        nestedScrollConnection: NestedScrollConnection? = null,
        onClickNav: () -> Unit = {},
        isRoot: () -> Boolean = { true },
        title: () -> String? = { null },
        query: @Composable () -> String = { "" },
        onQueryChange: (String) -> Unit = {},
        filterData: () -> AnimeMediaFilterController.Data<MediaSortOption>,
        onRefresh: () -> Unit = {},
        content: @Composable () -> LazyPagingItems<AnimeMediaListScreen.Entry> = {
            flowOf(PagingData.empty<AnimeMediaListScreen.Entry>()).collectAsLazyPagingItems()
        },
        tagShown: () -> AnimeMediaFilterController.TagSection.Tag? = { null },
        onTagDismiss: () -> Unit = {},
        onTagClick: (tagId: String, tagName: String) -> Unit = { _, _ -> },
        onTagLongClick: (tagId: String) -> Unit = {},
        onMediaClick: (AnimeMediaListRow.Entry) -> Unit = {},
        onMediaLongClick: (AnimeMediaListRow.Entry) -> Unit = {},
        scrollStateSaver: ScrollStateSaver = ScrollStateSaver.STUB,
    ) {
        AnimeMediaFilterOptionsBottomPanel(
            topBar = {
                if (isRoot()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth()
                    ) {
                        TextField(
                            query(),
                            placeholder = { Text(stringResource(id = R.string.anime_search)) },
                            onValueChange = onQueryChange,
                            leadingIcon = { NavMenuIconButton(onClickNav) },
                            trailingIcon = {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = stringResource(
                                            R.string.anime_search_clear
                                        ),
                                    )
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                } else {
                    AppBar(
                        text = title().orEmpty(),
                        onClickBack = onClickNav,
                    )
                }
            },
            filterData = filterData,
            onTagLongClicked = onTagLongClick,
        ) {
            @Suppress("NAME_SHADOWING")
            val content = content()
            val refreshing = content.loadState.refresh is LoadState.Loading
            AnimeMediaListScreen(
                refreshing = refreshing,
                onRefresh = onRefresh,
                tagShown = tagShown,
                onTagDismiss = onTagDismiss,
                modifier = Modifier.padding(it)
                    .run {
                        if (nestedScrollConnection == null) this else nestedScroll(
                            nestedScrollConnection
                        )
                    },
            ) { onLongPressImage ->
                when (content.loadState.refresh) {
                    LoadState.Loading -> Unit
                    is LoadState.Error -> AnimeMediaListScreen.Error()
                    is LoadState.NotLoading -> {
                        if (content.itemCount == 0) {
                            AnimeMediaListScreen.NoResults()
                        } else {
                            LazyColumn(
                                state = scrollStateSaver.lazyListState(),
                                contentPadding = PaddingValues(
                                    horizontal = 16.dp,
                                    vertical = 16.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                items(content, { it.id.scopedId }) {
                                    when (it) {
                                        is AnimeMediaListScreen.Entry.Item -> AnimeMediaListRow(
                                            entry = it,
                                            onClick = onMediaClick,
                                            onLongClick = onMediaLongClick,
                                            onTagClick = onTagClick,
                                            onTagLongClick = onTagLongClick,
                                            onLongPressImage = onLongPressImage,
                                        )
                                        null -> AnimeMediaListRow(AnimeMediaListRow.Entry.Loading)
                                    }
                                }

                                when (content.loadState.append) {
                                    is LoadState.Loading -> item(key = "load_more_append") {
                                        AnimeMediaListScreen.LoadingMore()
                                    }
                                    is LoadState.Error -> item(key = "load_more_error") {
                                        AnimeMediaListScreen.AppendError { content.retry() }
                                    }
                                    is LoadState.NotLoading -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    AnimeSearchScreen(
        content = {
            flowOf(
                PagingData.from<AnimeMediaListScreen.Entry>(
                    listOf(
                        AnimeMediaListScreen.Entry.Item(
                            Medium(
                                title = Medium.Title(
                                    userPreferred = "Ano Hi Mita Hana no Namae wo Bokutachi wa Mada Shiranai.",
                                ),
                            )
                        )
                    )
                )
            ).collectAsLazyPagingItems()
        },
        filterData = { AnimeMediaFilterController.Data.forPreview() }
    )
}
