package com.thekeeperofpie.artistalleydatabase.art

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


@Dao
interface ArtEntryDao {

    @Query("""SELECT * FROM art_entries WHERE art_entries.id = :id LIMIT 1""")
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
    fun getEntries(query: String = "'*'"): PagingSource<Int, ArtEntry>

    @Query(
        """
        SELECT *
        FROM art_entries
        """
    )
    fun getEntries(): PagingSource<Int, ArtEntry>

    @Query(
        """
        SELECT *
        FROM art_entries
        LIMIT :limit
        OFFSET :offset
        """
    )
    fun getEntries(limit: Int = 50, offset: Int = 0): List<ArtEntry>

    @Transaction
    fun iterateEntries(limit: Int = 50, block: (index: Int, entry: ArtEntry) -> Unit) {
        var offset = 0
        var index = 0
        var entries = getEntries(limit = limit, offset = offset)
        while (entries.isNotEmpty()) {
            offset += entries.size
            entries.forEach {
                block(index++, it)
            }
            entries = getEntries(limit = limit, offset = offset)
        }
    }

    suspend fun getEntries(
        orderBy: String = "date",
        ascending: Boolean = false,
        query: String = ""
    ): PagingSource<Int, ArtEntry> {
        val orderDirection = if (ascending) "ASC" else "DESC"
        val statement =
            "SELECT * FROM art_entries ORDER BY :orderBy $orderDirection WHERE * MATCH ':query'"
        return getEntries(SimpleSQLiteQuery(statement, arrayOf(orderBy, query)))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(vararg entries: ArtEntry)

    @Delete
    suspend fun delete(entry: ArtEntry) = delete(entry.id)

    @Query("DELETE FROM art_entries WHERE id = :id")
    suspend fun delete(id: String)
}