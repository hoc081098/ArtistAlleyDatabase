package com.thekeeperofpie.artistalleydatabase.anilist.oauth

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thekeeperofpie.artistalleydatabase.android_utils.kotlin.CustomDispatchers
import com.thekeeperofpie.artistalleydatabase.anilist.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AniListOAuthViewModel @Inject constructor(
    private val oAuthStore: AniListOAuthStore,
) : ViewModel() {

    var state by mutableStateOf<State>(State.Loading)

    private var initialized = false

    fun initialize(text: String?) {
        if (initialized) return
        initialized = true
        viewModelScope.launch(CustomDispatchers.IO) {
            val state = if (text == null) {
                State.Error(R.string.aniList_oAuth_error)
            } else if (text.startsWith("http")) {
                if (!text.contains("/api/v2/oauth/pin")) {
                    State.Error(R.string.aniList_oAuth_error_bad_url)
                } else {
                    try {
                        // AniList token page exposes access_token inside the URL fragment
                        val token = Uri.parse("text://host?${Uri.parse(text).encodedFragment}")
                            .getQueryParameter("access_token")
                        if (token == null) {
                            State.Error(R.string.aniList_oAuth_error_bad_url)
                        } else {
                            oAuthStore.storeAuthTokenResult(token)
                            State.Done
                        }
                    } catch (exception: Exception) {
                        State.Error(R.string.aniList_oAuth_error, exception)
                    }
                }
            } else {
                oAuthStore.storeAuthTokenResult(text)
                State.Done
            }

            launch(CustomDispatchers.Main) {
                this@AniListOAuthViewModel.state = state
            }
        }
    }

    sealed interface State {
        data object Loading : State
        data object Done : State
        data class Error(@StringRes val textRes: Int, val exception: Exception? = null) : State
    }
}
