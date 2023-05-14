package com.thekeeperofpie.artistalleydatabase.anime.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thekeeperofpie.artistalleydatabase.android_utils.kotlin.CustomDispatchers
import com.thekeeperofpie.artistalleydatabase.anilist.oauth.AuthedAniListApi
import com.thekeeperofpie.artistalleydatabase.anime.R
import com.thekeeperofpie.artistalleydatabase.anime.user.stats.UserStatsGenreState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AniListUserViewModel @Inject constructor(
    private val aniListApi: AuthedAniListApi,
) : ViewModel() {

    private var initialized = false
    private var userId: String? = null

    val entry = MutableStateFlow<AniListUserScreen.Entry?>(null)
    val viewer = aniListApi.authedUser
    var errorResource = MutableStateFlow<Pair<Int, Exception?>?>(null)

    val animeGenreState = UserStatsGenreState(viewModelScope, aniListApi, isAnime = true)
    val mangaGenreState = UserStatsGenreState(viewModelScope, aniListApi, isAnime = false)

    private val refreshUptimeMillis = MutableStateFlow(-1L)

    fun initialize(userId: String?) {
        if (initialized) return
        initialized = true
        this.userId = userId

        viewModelScope.launch(CustomDispatchers.IO) {
            refreshUptimeMillis.collectLatest {
                try {
                    entry.value =
                        aniListApi.user((userId ?: aniListApi.authedUser.value?.id?.toString())!!)
                            ?.let(AniListUserScreen::Entry)
                } catch (e: Exception) {
                    errorResource.value = R.string.anime_user_error_loading to e
                }
            }
        }
    }
}
