package com.thekeeperofpie.artistalleydatabase.art.details

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Monitor
import com.anilist.fragment.AniListCharacter
import com.anilist.fragment.AniListMedia
import com.anilist.type.MediaType
import com.thekeeperofpie.artistalleydatabase.R
import com.thekeeperofpie.artistalleydatabase.anilist.AniListUtils
import com.thekeeperofpie.artistalleydatabase.anilist.CharacterColumnEntry
import com.thekeeperofpie.artistalleydatabase.anilist.MediaColumnEntry
import com.thekeeperofpie.artistalleydatabase.anilist.character.CharacterEntry
import com.thekeeperofpie.artistalleydatabase.anilist.character.CharacterUtils
import com.thekeeperofpie.artistalleydatabase.anilist.media.MediaEntry
import com.thekeeperofpie.artistalleydatabase.json.AppMoshi
import com.thekeeperofpie.artistalleydatabase.utils.Either
import javax.inject.Inject

class ArtEntryDataConverter @Inject constructor(
    private val appMoshi: AppMoshi
) {

    fun databaseToSeriesEntry(value: String) =
        when (val either = appMoshi.parseSeriesColumn(value)) {
            is Either.Right -> seriesEntry(either.value)
            is Either.Left -> ArtEntrySection.MultiText.Entry.Custom(either.value)
        }

    fun databaseToCharacterEntry(value: String) =
        when (val either = appMoshi.parseCharacterColumn(value)) {
            is Either.Right -> characterEntry(either.value)
            is Either.Left -> ArtEntrySection.MultiText.Entry.Custom(either.value)
        }

    fun seriesEntry(media: AniListMedia): ArtEntrySection.MultiText.Entry.Prefilled {
        val title = media.title?.romaji ?: media.id.toString()
        val serializedValue = appMoshi.toJson(MediaColumnEntry(media.id, title.trim()))
        return ArtEntrySection.MultiText.Entry.Prefilled(
            id = media.id.toString(),
            text = title,
            trailingIcon = when (media.type) {
                MediaType.ANIME -> Icons.Default.Monitor
                MediaType.MANGA -> Icons.Default.Book
                else -> null
            },
            trailingIconContentDescription = when (media.type) {
                MediaType.ANIME -> R.string.aniList_entry_anime_indicator_content_description
                MediaType.MANGA -> R.string.aniList_entry_manga_indicator_content_description
                else -> null
            },
            image = media.coverImage?.medium,
            imageLink = AniListUtils.mediaUrl(media.type, media.id),
            serializedValue = serializedValue,
            searchableValue = (listOf(
                media.title?.romaji,
                media.title?.english,
                media.title?.native,
            ) + media.synonyms.orEmpty())
                .filterNot(String?::isNullOrBlank)
                .mapNotNull { it?.trim() }
                .joinToString()
        )
    }

    fun seriesEntry(entry: MediaEntry): ArtEntrySection.MultiText.Entry {
        val title = entry.title
        val nonNullTitle = title?.romaji ?: entry.id.toString()
        val serializedValue = appMoshi.toJson(MediaColumnEntry(entry.id, nonNullTitle))
        return ArtEntrySection.MultiText.Entry.Prefilled(
            id = entry.id.toString(),
            text = nonNullTitle,
            trailingIcon = when (entry.type) {
                MediaEntry.Type.ANIME -> Icons.Default.Monitor
                MediaEntry.Type.MANGA -> Icons.Default.Book
                else -> null
            },
            trailingIconContentDescription = when (entry.type) {
                MediaEntry.Type.ANIME -> R.string.aniList_entry_anime_indicator_content_description
                MediaEntry.Type.MANGA -> R.string.aniList_entry_manga_indicator_content_description
                else -> null
            },
            image = entry.image?.medium,
            imageLink = AniListUtils.mediaUrl(entry.type, entry.id),
            serializedValue = serializedValue,
            searchableValue = (listOf(
                title?.romaji,
                title?.english,
                title?.native
            ) + entry.synonyms.orEmpty())
                .filterNot(String?::isNullOrBlank)
                .joinToString()
        )
    }

    fun seriesEntry(entry: MediaColumnEntry): ArtEntrySection.MultiText.Entry {
        val serializedValue = appMoshi.toJson(MediaColumnEntry(entry.id, entry.title))
        return ArtEntrySection.MultiText.Entry.Prefilled(
            id = entry.id.toString(),
            text = entry.title,
            image = null,
            imageLink = null,
            serializedValue = serializedValue,
            searchableValue = entry.title.trim()
        )
    }

    fun characterEntry(character: AniListCharacter) =
        characterEntry(
            id = character.id,
            image = character.image?.medium,
            first = character.name?.first,
            middle = character.name?.middle,
            last = character.name?.last,
            full = character.name?.full,
            native = character.name?.native,
            alternative = character.name?.alternative?.filterNotNull(),
            mediaTitle = character.media?.nodes?.firstOrNull()?.aniListMedia?.title?.romaji
        )

    fun characterEntry(entry: CharacterColumnEntry) =
        characterEntry(
            id = entry.id,
            image = null,
            first = entry.name?.first,
            middle = entry.name?.middle,
            last = entry.name?.last,
            full = entry.name?.full,
            native = entry.name?.native,
            alternative = emptyList(),
            mediaTitle = null,
        )

    fun characterEntry(entry: CharacterEntry, media: List<MediaEntry>) =
        characterEntry(
            id = entry.id,
            image = entry.image?.medium,
            first = entry.name?.first,
            middle = entry.name?.middle,
            last = entry.name?.last,
            full = entry.name?.full,
            native = entry.name?.native,
            alternative = entry.name?.alternative,
            mediaTitle = media.firstOrNull()?.title?.romaji,
        )

    private fun characterEntry(
        id: Int,
        image: String?,
        first: String?,
        middle: String?,
        last: String?,
        full: String?,
        native: String?,
        alternative: List<String>?,
        mediaTitle: String?
    ): ArtEntrySection.MultiText.Entry.Prefilled {
        val canonicalName = CharacterUtils.buildCanonicalName(
            first = first,
            middle = middle,
            last = last,
        ) ?: id.toString()

        val displayName = CharacterUtils.buildDisplayName(canonicalName, alternative)

        val serializedValue = appMoshi.toJson(
            CharacterColumnEntry(
                id, CharacterColumnEntry.Name(
                    first = first?.trim(),
                    middle = middle?.trim(),
                    last = last?.trim(),
                    full = full?.trim(),
                    native = native?.trim(),
                )
            )
        )
        return ArtEntrySection.MultiText.Entry.Prefilled(
            id = id.toString(),
            text = canonicalName,
            image = image,
            imageLink = AniListUtils.characterUrl(id),
            titleText = displayName,
            subtitleText = mediaTitle,
            serializedValue = serializedValue,
            searchableValue = (listOf(last, middle, first) + alternative.orEmpty())
                .filterNot(String?::isNullOrBlank)
                .mapNotNull { it?.trim() }
                .joinToString()
        )
    }
}