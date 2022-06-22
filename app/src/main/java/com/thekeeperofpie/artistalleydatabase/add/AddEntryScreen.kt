package com.thekeeperofpie.artistalleydatabase.add

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.thekeeperofpie.artistalleydatabase.R
import com.thekeeperofpie.artistalleydatabase.art.ArtEntryForm
import com.thekeeperofpie.artistalleydatabase.art.ArtEntrySection
import com.thekeeperofpie.artistalleydatabase.art.ImagesSelectBox
import com.thekeeperofpie.artistalleydatabase.art.SampleArtEntrySectionsProvider
import com.thekeeperofpie.artistalleydatabase.ui.ButtonFooter
import com.thekeeperofpie.artistalleydatabase.ui.SnackbarErrorText
import com.thekeeperofpie.artistalleydatabase.ui.theme.ArtistAlleyDatabaseTheme

object AddEntryScreen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    operator fun invoke(
        imageUris: List<Uri> = emptyList(),
        onImagesSelected: (List<Uri>) -> Unit = {},
        onImageSelectError: (Exception?) -> Unit = {},
        onImageSizeResult: (Int, Int) -> Unit = { _, _ -> },
        sections: List<ArtEntrySection> = emptyList(),
        onClickSave: () -> Unit = {},
        errorRes: Pair<Int, Exception?>? = null,
        onErrorDismiss: () -> Unit = {},
    ) {
        ArtistAlleyDatabaseTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Scaffold(snackbarHost = {
                    SnackbarErrorText(errorRes?.first, onErrorDismiss = onErrorDismiss)
                }) {
                    Column(Modifier.padding(it)) {
                        Column(
                            modifier = Modifier
                                .weight(1f, true)
                                .verticalScroll(rememberScrollState())
                        ) {
                            HeaderImage(
                                imageUris,
                                onImagesSelected,
                                onImageSelectError,
                                onImageSizeResult,
                            )

                            ArtEntryForm(false, sections)
                        }

                        ButtonFooter(onClickSave, R.string.save)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    @Composable
    private fun HeaderImage(
        imageUris: List<Uri> = emptyList(),
        onImagesSelected: (List<Uri>) -> Unit = {},
        onImageSelectError: (Exception?) -> Unit = {},
        onImageSizeResult: (Int, Int) -> Unit = { _, _ -> },
    ) {
        ImagesSelectBox(onImagesSelected, onImageSelectError) {
            if (imageUris.isNotEmpty()) {
                val pagerState = rememberPagerState()
                HorizontalPager(
                    state = pagerState,
                    count = imageUris.size,
                    modifier = Modifier.heightIn(min = 200.dp, max = 400.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUris[it])
                            .crossfade(true)
                            .listener { _, result ->
                                onImageSizeResult(
                                    result.drawable.intrinsicWidth,
                                    result.drawable.intrinsicHeight,
                                )
                            }
                            .build(),
                        contentDescription = stringResource(
                            R.string.art_entry_image_content_description
                        ),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                if (imageUris.size > 1) {
                    HorizontalPagerIndicator(
                        pagerState = pagerState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                    )
                }
            } else {
                Spacer(
                    Modifier
                        .heightIn(200.dp, 200.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray)
                )
                Icon(
                    imageVector = Icons.Default.ImageSearch,
                    contentDescription = stringResource(
                        R.string.add_entry_select_image_content_description
                    ),
                    Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Preview
@Composable
fun Preview(
    @PreviewParameter(SampleArtEntrySectionsProvider::class) sections: List<ArtEntrySection>
) {
    AddEntryScreen(sections = sections)
}