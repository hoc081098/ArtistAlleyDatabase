package com.thekeeperofpie.artistalleydatabase.art

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.paging.compose.collectAsLazyPagingItems
import com.thekeeperofpie.artistalleydatabase.android_utils.Either
import com.thekeeperofpie.artistalleydatabase.art.browse.selection.ArtBrowseSelectionScreen
import com.thekeeperofpie.artistalleydatabase.art.browse.selection.ArtBrowseSelectionViewModel
import com.thekeeperofpie.artistalleydatabase.art.data.ArtEntryColumn
import com.thekeeperofpie.artistalleydatabase.art.search.ArtSearchViewModel
import com.thekeeperofpie.artistalleydatabase.art.utils.ArtEntryUtils
import com.thekeeperofpie.artistalleydatabase.browse.BrowseEntryModel
import com.thekeeperofpie.artistalleydatabase.browse.BrowseSelectionNavigator
import com.thekeeperofpie.artistalleydatabase.compose.AddBackPressInvokeFirst
import com.thekeeperofpie.artistalleydatabase.compose.BackPressStageHandler
import com.thekeeperofpie.artistalleydatabase.compose.LazyStaggeredGrid
import com.thekeeperofpie.artistalleydatabase.entry.EntryDetailsScreen
import com.thekeeperofpie.artistalleydatabase.entry.EntryHomeScreen
import com.thekeeperofpie.artistalleydatabase.entry.EntryId
import com.thekeeperofpie.artistalleydatabase.entry.EntryUtils.entryDetailsComposable
import com.thekeeperofpie.artistalleydatabase.entry.EntryUtils.navToEntryDetails

class ArtEntryNavigator : BrowseSelectionNavigator {

    fun initialize(
        onClickNav: () -> Unit,
        navHostController: NavHostController,
        navGraphBuilder: NavGraphBuilder,
    ) {
        navGraphBuilder.composable(ArtNavDestinations.HOME.id) {
            val viewModel = hiltViewModel<ArtSearchViewModel>()
            val lazyStaggeredGridState =
                LazyStaggeredGrid.rememberLazyStaggeredGridState(columnCount = 2)
            EntryHomeScreen(
                screenKey = ArtNavDestinations.HOME.id,
                onClickNav = onClickNav,
                query = { viewModel.query?.query.orEmpty() },
                entriesSize = { viewModel.entriesSize },
                onQueryChange = viewModel::onQuery,
                options = { viewModel.options },
                onOptionChange = { viewModel.refreshQuery() },
                entries = { viewModel.results.collectAsLazyPagingItems() },
                selectedItems = { viewModel.selectedEntries.keys },
                onClickAddFab = {
                    navHostController.navToEntryDetails(
                        route = ArtNavDestinations.ENTRY_DETAILS.id,
                        emptyList(),
                    )
                },
                onClickEntry = { index, entry ->
                    if (viewModel.selectedEntries.isNotEmpty()) {
                        viewModel.selectEntry(index, entry)
                    } else {
                        navHostController.navToEntryDetails(
                            route = ArtNavDestinations.ENTRY_DETAILS.id,
                            listOf(entry.id.valueId)
                        )
                    }
                },
                onLongClickEntry = viewModel::selectEntry,
                onClickClear = viewModel::clearSelected,
                onClickEdit = {
                    navHostController.navToEntryDetails(
                        ArtNavDestinations.ENTRY_DETAILS.id,
                        viewModel.selectedEntries.values.map { it.id.valueId }
                    )
                },
                onConfirmDelete = viewModel::deleteSelected,
                lazyStaggeredGridState = lazyStaggeredGridState,
            )
        }

        navGraphBuilder.composable(
            ArtNavDestinations.BROWSE_SELECTION.id +
                    "?queryType={queryType}" +
                    "&title={title}" +
                    "&queryId={queryId}" +
                    "&queryString={queryString}",
            arguments = listOf(
                navArgument("queryType") {
                    type = NavType.StringType
                },
                navArgument("title") {
                    type = NavType.StringType
                },
                navArgument("queryId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("queryString") {
                    type = NavType.StringType
                    nullable = true
                },
            )
        ) {
            val arguments = it.arguments!!
            val column = ArtEntryColumn.valueOf(arguments.getString("queryType")!!)
            val title = arguments.getString("title")!!
            val queryId = arguments.getString("queryId")
            val queryString = arguments.getString("queryString")
            val viewModel = hiltViewModel<ArtBrowseSelectionViewModel>()
            val query: Either<String, String> = if (queryId != null) {
                Either.Left(queryId)
            } else {
                Either.Right(queryString!!)
            }

            viewModel.initialize(column, query)
            ArtBrowseSelectionScreen(
                title = { title },
                loading = { viewModel.loading },
                entries = { viewModel.entries.collectAsLazyPagingItems() },
                selectedItems = { viewModel.selectedEntries.keys },
                onClickEntry = { index, entry ->
                    if (viewModel.selectedEntries.isNotEmpty()) {
                        viewModel.selectEntry(index, entry)
                    } else {
                        navHostController.navToEntryDetails(
                            ArtNavDestinations.ENTRY_DETAILS.id,
                            listOf(entry.id.valueId)
                        )
                    }
                },
                onLongClickEntry = viewModel::selectEntry,
                onClickClear = viewModel::clearSelected,
                onClickEdit = {
                    navHostController.navToEntryDetails(
                        ArtNavDestinations.ENTRY_DETAILS.id,
                        viewModel.selectedEntries.values.map { it.id.valueId }
                    )
                },
                onConfirmDelete = viewModel::onDeleteSelected,
            )
        }

        navGraphBuilder.entryDetailsComposable(
            route = ArtNavDestinations.ENTRY_DETAILS.id
        ) { entryIds, imageCornerDp ->
            val viewModel = hiltViewModel<ArtEntryDetailsViewModel>()
                .apply { initialize(entryIds.map { EntryId(ArtEntryUtils.SCOPED_ID_TYPE, it) }) }

            BackPressStageHandler {
                AddBackPressInvokeFirst(label = "ArtEntryNavigator exit") {
                    viewModel.onNavigateBack()
                }

                val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current
                    ?.onBackPressedDispatcher

                EntryDetailsScreen(
                    screenKey = ArtNavDestinations.ENTRY_DETAILS.id,
                    viewModel = viewModel,
                    onClickBack = { navHostController.popBackStack() },
                    imageCornerDp = imageCornerDp,
                    onImageClickOpen = {
                        viewModel.entryImageController.onImageClickOpen(navHostController, it)
                    },
                    onClickSave = { viewModel.onClickSave(navHostController) },
                    onLongClickSave = { viewModel.onLongClickSave(navHostController) },
                    onConfirmDelete = { viewModel.onConfirmDelete(navHostController) },
                    onClickSaveTemplate = { viewModel.onClickSaveTemplate() },
                    onExitConfirm = { backPressedDispatcher?.let(viewModel::onExitConfirm) },
                )
            }
        }
    }

    override fun navigate(navHostController: NavHostController, entry: BrowseEntryModel) {
        val query = entry.queryIdOrString
        val queryParam = if (query is Either.Left) {
            "&queryId=${query.value}"
        } else {
            "&queryString=${query.rightOrNull()}"
        }
        navHostController.navigate(
            ArtNavDestinations.BROWSE_SELECTION.id +
                    "?queryType=${entry.queryType}" +
                    "&title=${entry.text}" +
                    queryParam
        )
    }
}
