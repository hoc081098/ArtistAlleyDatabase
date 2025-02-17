package com.thekeeperofpie.artistalleydatabase.ui.theme

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.UriHandler
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import com.thekeeperofpie.artistalleydatabase.CustomApplication
import com.thekeeperofpie.artistalleydatabase.R
import com.thekeeperofpie.artistalleydatabase.android_utils.UriUtils

@Composable
fun ArtistAlleyDatabaseTheme(
    navHostController: NavHostController,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) darkColorScheme() else lightColorScheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(
                (view.context as Activity).window,
                view
            ).isAppearanceLightStatusBars = darkTheme
        }
    }

    val uriHandler = object : UriHandler {
        override fun openUri(uri: String) {
            try {
                val deepLinkUri = Uri.parse(uri)
                if (deepLinkUri.getQueryParameter(UriUtils.FORCE_EXTERNAL_URI_PARAM) != "true"
                    && navHostController.graph.hasDeepLink(deepLinkUri)
                ) {
                    navHostController.navigate(deepLinkUri)
                } else {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                }
            } catch (e: Exception) {
                Log.d(CustomApplication.TAG, "Error launching URI $uri", e)
                Toast.makeText(context, R.string.error_launching_generic_uri, Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    CompositionLocalProvider(
        LocalUriHandler provides uriHandler
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
