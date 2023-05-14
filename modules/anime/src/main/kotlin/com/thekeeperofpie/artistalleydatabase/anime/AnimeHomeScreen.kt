package com.thekeeperofpie.artistalleydatabase.anime

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anilist.type.MediaType
import com.thekeeperofpie.artistalleydatabase.android_utils.UtilsStringR
import com.thekeeperofpie.artistalleydatabase.anime.media.AnimeMediaListRow
import com.thekeeperofpie.artistalleydatabase.anime.user.AniListUserScreen
import com.thekeeperofpie.artistalleydatabase.compose.EnterAlwaysNavigationBar
import com.thekeeperofpie.artistalleydatabase.compose.ScrollStateSaver
import com.thekeeperofpie.artistalleydatabase.compose.SnackbarErrorText
import com.thekeeperofpie.artistalleydatabase.compose.navigationBarEnterAlwaysScrollBehavior

@OptIn(ExperimentalMaterial3Api::class)
object AnimeHomeScreen {

    @Composable
    operator fun invoke(
        onClickNav: () -> Unit,
        needAuth: () -> Boolean,
        onClickAuth: () -> Unit,
        onSubmitAuthToken: (String) -> Unit,
        onTagClick: (tagId: String, tagName: String) -> Unit,
        onMediaClick: (AnimeMediaListRow.Entry) -> Unit,
        userCallback: AniListUserScreen.Callback,
        errorRes: () -> Pair<Int, Exception?>? = { null },
        onErrorDismiss: () -> Unit = { },
    ) {
        var selectedScreen by rememberSaveable(stateSaver = object :
            Saver<AnimeNavDestinations, String> {
            override fun restore(value: String) =
                AnimeNavDestinations.values().find { it.id == value } ?: AnimeNavDestinations.SEARCH

            override fun SaverScope.save(value: AnimeNavDestinations) = value.id
        }) { mutableStateOf(AnimeNavDestinations.SEARCH) }

        val scrollBehavior = navigationBarEnterAlwaysScrollBehavior()

        @Suppress("UnusedMaterial3ScaffoldPaddingParameter")
        Scaffold(
            snackbarHost = {
                SnackbarErrorText(
                    errorRes()?.first,
                    errorRes()?.second,
                    onErrorDismiss = onErrorDismiss
                )
            },
            bottomBar = {
                EnterAlwaysNavigationBar(scrollBehavior = scrollBehavior) {
                    AnimeNavDestinations.values().forEach { destination ->
                        NavigationBarItem(
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.textRes)) },
                            selected = selectedScreen == destination,
                            onClick = { selectedScreen = destination }
                        )
                    }
                }
            },
        ) {
            @Composable
            fun bottomNavBarPadding(): Dp {
                val density = LocalDensity.current
                return remember {
                    derivedStateOf {
                        scrollBehavior.state.heightOffsetLimit
                            .takeUnless { it == -Float.MAX_VALUE }
                            ?.let { density.run { -it.toDp() } }
                            ?: 80.dp
                    }
                }.value
            }

            @Composable
            fun bottomOffset(): Dp {
                val density = LocalDensity.current
                return remember {
                    derivedStateOf {
                        density.run { scrollBehavior.state.heightOffset.toDp() }
                    }
                }.value
            }

            val scrollPositions = ScrollStateSaver.scrollPositions()
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (needAuth()) {
                    AuthPrompt(onClickAuth = onClickAuth, onSubmitAuthToken = onSubmitAuthToken)
                } else {
                    AnimatedContent(
                        targetState = selectedScreen,
                        transitionSpec = {
                            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
                                .togetherWith(
                                    slideOutOfContainer(
                                        AnimatedContentTransitionScope.SlideDirection.Down
                                    )
                                )
                        },
                        label = "Anime home destination transition",
                    ) {
                        when (it) {
                            AnimeNavDestinations.ANIME -> AnimeNavigator.UserListScreen(
                                userId = null,
                                mediaType = MediaType.ANIME,
                                onClickNav = onClickNav,
                                showDrawerHandle = true,
                                onTagClick = onTagClick,
                                onMediaClick = onMediaClick,
                                scrollStateSaver = ScrollStateSaver.fromMap(
                                    AnimeNavDestinations.ANIME.id,
                                    scrollPositions
                                ),
                                nestedScrollConnection = scrollBehavior.nestedScrollConnection,
                                bottomNavBarPadding = { bottomNavBarPadding() },
                                bottomOffset = { bottomOffset() },
                            )
                            AnimeNavDestinations.MANGA -> AnimeNavigator.UserListScreen(
                                userId = null,
                                mediaType = MediaType.MANGA,
                                onClickNav = onClickNav,
                                showDrawerHandle = true,
                                onTagClick = onTagClick,
                                onMediaClick = onMediaClick,
                                scrollStateSaver = ScrollStateSaver.fromMap(
                                    AnimeNavDestinations.MANGA.id,
                                    scrollPositions
                                ),
                                nestedScrollConnection = scrollBehavior.nestedScrollConnection,
                                bottomNavBarPadding = { bottomNavBarPadding() },
                                bottomOffset = { bottomOffset() },
                            )
                            AnimeNavDestinations.SEARCH -> AnimeNavigator.SearchScreen(
                                title = null,
                                tagId = null,
                                onClickNav = onClickNav,
                                onTagClick = onTagClick,
                                onMediaClick = onMediaClick,
                                scrollStateSaver = ScrollStateSaver.fromMap(
                                    AnimeNavDestinations.SEARCH.id,
                                    scrollPositions
                                ),
                                nestedScrollConnection = scrollBehavior.nestedScrollConnection,
                                bottomNavBarPadding = { bottomNavBarPadding() },
                                bottomOffset = { bottomOffset() },
                            )
                            AnimeNavDestinations.PROFILE -> AnimeNavigator.UserScreen(
                                userId = null,
                                callback = userCallback,
                                nestedScrollConnection = scrollBehavior.nestedScrollConnection,
                                bottomNavBarPadding = { bottomNavBarPadding() },
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AuthPrompt(onClickAuth: () -> Unit, onSubmitAuthToken: (String) -> Unit) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.anime_auth_prompt))
            TextButton(onClick = onClickAuth) {
                Text(stringResource(R.string.anime_auth_button))
            }
            Text(stringResource(R.string.anime_auth_prompt_paste))

            var value by remember { mutableStateOf("") }
            TextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier
                    .sizeIn(minWidth = 200.dp, minHeight = 200.dp)
                    .padding(16.dp),
            )

            TextButton(onClick = {
                val token = value
                value = ""
                onSubmitAuthToken(token)
            }) {
                Text(stringResource(UtilsStringR.confirm))
            }
        }
    }
}
