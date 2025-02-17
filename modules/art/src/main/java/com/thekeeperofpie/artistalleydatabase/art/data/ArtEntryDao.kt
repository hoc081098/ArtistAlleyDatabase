package com.thekeeperofpie.artistalleydatabase.art.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.thekeeperofpie.artistalleydatabase.art.search.ArtSearchQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.yield

@Dao
interface ArtEntryDao {

    @Query("""SELECT * FROM art_entries WHERE id = :id""")
    suspend fun getEntry(id: String): ArtEntry

    @RawQuery([ArtEntry::class])
    fun getEntries(query: SupportSQLiteQuery): PagingSource<Int, ArtEntry>

    @Query(
        """
        SELECT *
        FROM art_entries
        JOIN art_entries_fts ON art_entries.id = art_entries_fts.id
        WHERE art_entries_fts MATCH :query
    """
    )
    fun getEntries(query: String): PagingSource<Int, ArtEntry>

    @Query(
        """
        SELECT *
        FROM art_entries
        ORDER BY lastEditTime DESC
        """
    )
    fun getEntries(): PagingSource<Int, ArtEntry>

    fun getEntries(query: ArtSearchQuery): PagingSource<Int, ArtEntry> {
        val includeAll = query.includeAll
        val lockedValue = when {
            includeAll -> null
            query.locked && query.unlocked -> null
            query.locked -> "1"
            query.unlocked -> "0"
            else -> null
        }

        val options = query.query.split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .map { "*$it*" }
            .map { queryValue ->
                mutableListOf<String>().apply {
                    if (includeAll || query.includeArtists) this += "artists:$queryValue"
                    if (includeAll || query.includeSources) this += "sourceType:$queryValue"
                    if (includeAll || query.includeSources) this += "sourceValue:$queryValue"
                    if (includeAll || query.includeSeries) this += "seriesSearchable:$queryValue"
                    if (includeAll || query.includeCharacters) this += "charactersSearchable:$queryValue"
                    if (includeAll || query.includeTags) this += "tags:$queryValue"
                    if (includeAll || query.includeNotes) this += "notes:$queryValue"
                }
            }

        if (options.isEmpty() && lockedValue == null) {
            return getEntries()
        }

        val lockOptions = if (lockedValue == null) emptyList() else {
            mutableListOf<String>().apply {
                if (query.includeArtists) this += "artistsLocked:$lockedValue"
                if (query.includeSources) this += "sourceLocked:$lockedValue"
                if (query.includeSeries) this += "seriesLocked:$lockedValue"
                if (query.includeCharacters) this += "charactersLocked:$lockedValue"
                if (query.includeTags) this += "tagsLocked:$lockedValue"
                if (query.includeNotes) this += "notesLocked:$lockedValue"
            }
        }

        val bindArguments = (options.ifEmpty { listOf(listOf("")) }).map {
            it.joinToString(separator = " OR ") + " " +
                    lockOptions.joinToString(separator = " ")
        }

        val statement = bindArguments.joinToString("\nINTERSECT\n") {
            """
                SELECT *
                FROM art_entries
                JOIN art_entries_fts ON art_entries.id = art_entries_fts.id
                WHERE art_entries_fts MATCH ?
                """.trimIndent()
        } + "\nORDER BY art_entries.lastEditTime DESC"

        return getEntries(SimpleSQLiteQuery(statement, bindArguments.toTypedArray()))
    }

    @Query(
        """
        SELECT *
        FROM art_entries
        LIMIT :limit
        OFFSET :offset
        """
    )
    suspend fun getEntries(limit: Int = 50, offset: Int = 0): List<ArtEntry>

    @Query(
        """
        SELECT COUNT(*)
        FROM art_entries
        """
    )
    fun getEntriesSize(): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM art_entries
        """
    )
    fun getEntriesSizeFlow(): Flow<Int>

    @Transaction
    suspend fun iterateEntries(
        entriesSize: (Int) -> Unit,
        limit: Int = 50,
        block: suspend (index: Int, entry: ArtEntry) -> Unit,
    ) {
        var offset = 0
        var index = 0
        entriesSize(getEntriesSize())
        var entries = getEntries(limit = limit, offset = offset)
        while (entries.isNotEmpty()) {
            offset += entries.size
            entries.forEach {
                block(index++, it)
                yield()
            }
            entries = getEntries(limit = limit, offset = offset)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(vararg entries: ArtEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: Collection<ArtEntry>)

    suspend fun insertEntriesDeferred(
        dryRun: Boolean,
        replaceAll: Boolean,
        block: suspend (insertEntry: suspend (ArtEntry) -> Unit) -> Unit
    ) {
        if (!dryRun && replaceAll) {
            deleteAll()
        }
        block { insertEntries(it) }
    }

    @Delete
    suspend fun delete(entry: ArtEntry) = delete(entry.id)

    @Delete
    suspend fun delete(entries: Collection<ArtEntry>)

    @Query("DELETE FROM art_entries WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM art_entries")
    suspend fun deleteAll()

    @Transaction
    suspend fun transaction(block: suspend () -> Unit) = block()
}
