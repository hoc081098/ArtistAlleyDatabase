package com.thekeeperofpie.artistalleydatabase.anime.media

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anilist.MediaDetailsQuery
import com.anilist.fragment.AniListListRowMedia
import com.thekeeperofpie.artistalleydatabase.anime.R
import com.thekeeperofpie.artistalleydatabase.compose.AssistChip
import com.thekeeperofpie.artistalleydatabase.compose.AutoHeightText
import com.thekeeperofpie.artistalleydatabase.compose.assistChipColors
import com.thekeeperofpie.artistalleydatabase.compose.ifNotSpecified

data class AnimeMediaTagEntry(
    val id: String,
    val name: String,
    val shouldHide: Boolean = false,
    val leadingIconVector: ImageVector? = null,
    val leadingIconContentDescription: Int? = null,
    val textHiddenRes: Int? = null,
    val rank: Int? = null,
) {
    companion object {
        @Composable
        fun Chip(
            tag: AnimeMediaTagEntry,
            modifier: Modifier = Modifier,
            title: @Composable () -> String = { tag.name },
            onTagClick: (tagId: String, tagName: String) -> Unit = { _, _ -> },
            onTagLongClick: (tagId: String) -> Unit = {},
            containerColor: Color,
            textColor: Color,
        ) {
            val shouldHide = tag.shouldHide
            var hidden by remember(tag.id) { mutableStateOf(shouldHide) }
            AssistChip(
                onClick = {
                    if (!hidden) {
                        onTagClick(tag.id, tag.name)
                    } else {
                        hidden = false
                    }
                },
                onLongClickLabel = stringResource(
                    R.string.anime_media_tag_long_click_content_description
                ),
                onLongClick = { onTagLongClick(tag.id) },
                colors = assistChipColors(
                    containerColor = containerColor
                        .ifNotSpecified { MaterialTheme.colorScheme.surfaceVariant },
                    labelColor = textColor.ifNotSpecified { MaterialTheme.colorScheme.onSurfaceVariant },
                ),
                border = null,
                leadingIcon = {
                    if (tag.leadingIconVector != null
                        && tag.leadingIconContentDescription != null
                    ) {
                        Icon(
                            painter = rememberVectorPainter(tag.leadingIconVector),
                            contentDescription = stringResource(
                                tag.leadingIconContentDescription
                            ),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .size(16.dp)
                        )
                    }
                },
                label = {
                    AutoHeightText(
                        text = if (hidden && tag.textHiddenRes != null) {
                            stringResource(tag.textHiddenRes)
                        } else {
                            title()
                        },
                    )
                },
                modifier = modifier,
            )
        }
    }

    constructor(tag: AniListListRowMedia.Tag) : this(
        id = tag.id.toString(),
        name = tag.name,
        shouldHide = (tag.isAdult ?: false)
                || (tag.isGeneralSpoiler ?: false)
                || (tag.isMediaSpoiler ?: false),
        leadingIconVector = MediaUtils.tagLeadingIcon(
            isAdult = tag.isAdult,
            isGeneralSpoiler = tag.isGeneralSpoiler,
            isMediaSpoiler = tag.isMediaSpoiler,
        ),
        leadingIconContentDescription = MediaUtils.tagLeadingIconContentDescription(
            isAdult = tag.isAdult,
            isGeneralSpoiler = tag.isGeneralSpoiler,
            isMediaSpoiler = tag.isMediaSpoiler,
        ),
        textHiddenRes = when {
            tag.isAdult ?: false -> R.string.anime_media_tag_adult
            (tag.isGeneralSpoiler ?: false) || (tag.isMediaSpoiler ?: false) ->
                R.string.anime_media_tag_spoiler
            else -> null
        },
    )

    constructor(tag: MediaDetailsQuery.Data.Media.Tag) : this(
        id = tag.id.toString(),
        name = tag.name,
        shouldHide = (tag.isAdult ?: false)
                || (tag.isGeneralSpoiler ?: false)
                || (tag.isMediaSpoiler ?: false),
        leadingIconVector = MediaUtils.tagLeadingIcon(
            isAdult = tag.isAdult,
            isGeneralSpoiler = tag.isGeneralSpoiler,
            isMediaSpoiler = tag.isMediaSpoiler,
        ),
        leadingIconContentDescription = MediaUtils.tagLeadingIconContentDescription(
            isAdult = tag.isAdult,
            isGeneralSpoiler = tag.isGeneralSpoiler,
            isMediaSpoiler = tag.isMediaSpoiler,
        ),
        textHiddenRes = when {
            tag.isAdult ?: false -> R.string.anime_media_tag_adult
            (tag.isGeneralSpoiler ?: false) || (tag.isMediaSpoiler ?: false) ->
                R.string.anime_media_tag_spoiler
            else -> null
        },
        rank = tag.rank,
    )
}
