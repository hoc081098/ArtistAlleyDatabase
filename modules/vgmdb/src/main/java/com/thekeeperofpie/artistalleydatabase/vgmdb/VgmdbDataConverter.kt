package com.thekeeperofpie.artistalleydatabase.vgmdb

import com.thekeeperofpie.artistalleydatabase.android_utils.Either
import com.thekeeperofpie.artistalleydatabase.form.EntrySection
import com.thekeeperofpie.artistalleydatabase.vgmdb.album.AlbumColumnEntry
import com.thekeeperofpie.artistalleydatabase.vgmdb.album.AlbumEntry
import com.thekeeperofpie.artistalleydatabase.vgmdb.artist.ArtistColumnEntry
import com.thekeeperofpie.artistalleydatabase.vgmdb.artist.ArtistEntry
import kotlinx.serialization.encodeToString
import javax.inject.Inject

class VgmdbDataConverter @Inject constructor(
    private val vgmdbJson: VgmdbJson,
) {

    fun catalogIdPlaceholder(
        album: SearchResults.AlbumResult
    ): EntrySection.MultiText.Entry.Prefilled<SearchResults.AlbumResult> {
        val catalogId = album.catalogId
        val titleText: String
        val subtitleText: String?
        if (catalogId == null) {
            titleText = album.name
            subtitleText = null
        } else {
            titleText = catalogId
            subtitleText = album.name
        }

        val serializedValue = vgmdbJson.json
            .encodeToString(AlbumColumnEntry(album.id, album.catalogId, titleText))

        return EntrySection.MultiText.Entry.Prefilled(
            value = album,
            id = albumEntryId(album.id),
            text = titleText,
            titleText = titleText,
            subtitleText = subtitleText,
            imageLink = "https://vgmdb.net/album/${album.id}",
            serializedValue = serializedValue,
            searchableValue = album.names.values
                .filterNot(String?::isNullOrBlank)
                .joinToString { it.trim() }
        )
    }

    fun catalogEntry(album: AlbumEntry): EntrySection.MultiText.Entry.Prefilled<AlbumEntry> {
        val catalogId = album.catalogId
        val titleText: String
        val subtitleText: String?
        if (catalogId == null) {
            titleText = album.title
            subtitleText = null
        } else {
            titleText = catalogId
            subtitleText = album.title
        }

        val serializedValue = vgmdbJson.json
            .encodeToString(AlbumColumnEntry(album.id, album.catalogId, album.title))

        return EntrySection.MultiText.Entry.Prefilled(
            value = album,
            id = albumEntryId(album.id),
            text = titleText,
            subtitleText = subtitleText,
            image = album.coverThumb,
            imageLink = "https://vgmdb.net/album/${album.id}",
            serializedValue = serializedValue,
            searchableValue = album.names.values
                .filterNot(String?::isNullOrBlank)
                .joinToString { it.trim() }
        )
    }

    fun catalogEntry(album: AlbumColumnEntry): EntrySection.MultiText.Entry {
        val catalogId = album.catalogId
        val titleText: String
        val subtitleText: String?
        if (catalogId == null) {
            titleText = album.title
            subtitleText = null
        } else {
            titleText = catalogId
            subtitleText = album.title
        }

        val serializedValue = vgmdbJson.json
            .encodeToString(AlbumColumnEntry(album.id, album.catalogId, album.title))

        return EntrySection.MultiText.Entry.Prefilled(
            value = album,
            id = albumEntryId(album.id),
            text = titleText,
            subtitleText = subtitleText,
            imageLink = "https://vgmdb.net/album/${album.id}",
            serializedValue = serializedValue,
            searchableValue = album.title,
        )
    }

    fun titleEntry(album: AlbumEntry): EntrySection.MultiText.Entry.Prefilled<AlbumEntry> {
        val serializedValue = vgmdbJson.json
            .encodeToString(AlbumColumnEntry(album.id, album.catalogId, album.title))

        return EntrySection.MultiText.Entry.Prefilled(
            value = album,
            id = albumEntryId(album.id),
            text = album.title,
            image = album.coverThumb,
            imageLink = "https://vgmdb.net/album/${album.id}",
            serializedValue = serializedValue,
            searchableValue = album.names.values
                .filterNot(String?::isNullOrBlank)
                .joinToString { it.trim() }
        )
    }

    fun titleEntry(album: AlbumColumnEntry): EntrySection.MultiText.Entry {
        val serializedValue = vgmdbJson.json
            .encodeToString(AlbumColumnEntry(album.id, album.catalogId, album.title))

        return EntrySection.MultiText.Entry.Prefilled(
            value = album,
            id = albumEntryId(album.id),
            text = album.title,
            imageLink = "https://vgmdb.net/album/${album.id}",
            serializedValue = serializedValue,
            searchableValue = album.title,
        )
    }

    fun databaseToCatalogIdEntry(value: String?) =
        when (val either = vgmdbJson.parseCatalogIdColumn(value)) {
            is Either.Right -> catalogEntry(either.value)
            is Either.Left -> EntrySection.MultiText.Entry.Custom(either.value)
        }

    fun databaseToTitleEntry(value: String?) =
        when (val either = vgmdbJson.parseTitleColumn(value)) {
            is Either.Right -> titleEntry(either.value)
            is Either.Left -> EntrySection.MultiText.Entry.Custom(either.value)
        }

    fun databaseToVocalistEntry(value: String) =
        when (val either = vgmdbJson.parseVocalistColumn(value)) {
            is Either.Right -> vocalistPlaceholder(either.value)
            is Either.Left -> EntrySection.MultiText.Entry.Custom(either.value)
        }

    fun databaseToComposerEntry(value: String) =
        when (val either = vgmdbJson.parseComposerColumn(value)) {
            is Either.Right -> composerPlaceholder(either.value)
            is Either.Left -> EntrySection.MultiText.Entry.Custom(either.value)
        }

    fun databaseToDiscEntry(value: String) = vgmdbJson.parseDiscColumn(value)

    fun artistPlaceholder(
        artist: SearchResults.ArtistResult
    ): EntrySection.MultiText.Entry.Prefilled<SearchResults.ArtistResult> {
        val serializedValue =
            vgmdbJson.json.encodeToString(ArtistColumnEntry(artist.id, mapOf("en" to artist.name)))
        return EntrySection.MultiText.Entry.Prefilled(
            value = artist,
            id = artistEntryId(artist.id),
            text = artist.name,
            imageLink = "https://vgmdb.net/artist/${artist.id}",
            serializedValue = serializedValue,
            searchableValue = artist.name
        )
    }

    fun vocalistPlaceholder(artist: ArtistColumnEntry) = artistPlaceholder(artist)
    fun vocalistEntry(artist: ArtistEntry) = artistEntry(artist)
    fun composerPlaceholder(artist: ArtistColumnEntry) = artistPlaceholder(artist)
    fun composerEntry(artist: ArtistEntry) = artistEntry(artist)

    private fun artistPlaceholder(
        artist: ArtistColumnEntry
    ): EntrySection.MultiText.Entry.Prefilled<ArtistColumnEntry> {
        val serializedValue = vgmdbJson.json.encodeToString(artist)
        return EntrySection.MultiText.Entry.Prefilled(
            value = artist,
            id = artistEntryId(artist.id),
            text = artist.name,
            imageLink = "https://vgmdb.net/artist/${artist.id}",
            serializedValue = serializedValue,
            searchableValue = artist.names.values
                .filterNot(String?::isNullOrBlank)
                .joinToString { it.trim() }
        )
    }

    private fun artistEntry(
        artist: ArtistEntry
    ): EntrySection.MultiText.Entry.Prefilled<ArtistEntry> {
        val serializedValue = vgmdbJson.json
            .encodeToString(ArtistColumnEntry(artist.id, artist.names))
        return EntrySection.MultiText.Entry.Prefilled(
            value = artist,
            id = artistEntryId(artist.id),
            text = artist.name,
            image = artist.pictureThumb,
            imageLink = "https://vgmdb.net/artist/${artist.id}",
            serializedValue = serializedValue,
            searchableValue = artist.names.values
                .filterNot(String?::isNullOrBlank)
                .joinToString { it.trim() }
        )
    }

    fun discEntries(album: AlbumEntry) = album.discs.mapNotNull(vgmdbJson::parseDiscColumn)

    private fun artistEntryId(id: String) = "vgmdbArtist_$id"
    private fun albumEntryId(id: String) = "vgmdbAlbum_$id"

    private val AlbumEntry.title get() = names["ja-latn"] ?: names["en"] ?: names["jp"] ?: ""

    private val SearchResults.AlbumResult.name
        get() = names["ja-latn"] ?: names["en"] ?: names["jp"] ?: ""

    private val ArtistColumnEntry.name
        get() = names["ja-latn"] ?: names["en"] ?: names["jp"] ?: names.values.firstOrNull() ?: ""

    private val ArtistEntry.name
        get() = names["ja-latn"] ?: names["en"] ?: names["jp"] ?: names.values.firstOrNull() ?: ""
}