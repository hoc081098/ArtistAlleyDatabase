package com.thekeeperofpie.artistalleydatabase.art

import android.app.Application
import com.squareup.moshi.Moshi
import com.thekeeperofpie.artistalleydatabase.android_utils.AppJson
import com.thekeeperofpie.artistalleydatabase.android_utils.persistence.DatabaseSyncer
import com.thekeeperofpie.artistalleydatabase.android_utils.persistence.Exporter
import com.thekeeperofpie.artistalleydatabase.android_utils.persistence.Importer
import com.thekeeperofpie.artistalleydatabase.anilist.character.CharacterEntryDao
import com.thekeeperofpie.artistalleydatabase.anilist.character.CharacterRepository
import com.thekeeperofpie.artistalleydatabase.anilist.media.MediaEntryDao
import com.thekeeperofpie.artistalleydatabase.anilist.media.MediaRepository
import com.thekeeperofpie.artistalleydatabase.art.browse.ArtBrowseTabArtists
import com.thekeeperofpie.artistalleydatabase.art.browse.ArtBrowseTabCharacters
import com.thekeeperofpie.artistalleydatabase.art.browse.ArtBrowseTabSeries
import com.thekeeperofpie.artistalleydatabase.art.browse.ArtBrowseTabTags
import com.thekeeperofpie.artistalleydatabase.art.data.ArtEntryBrowseDao
import com.thekeeperofpie.artistalleydatabase.art.data.ArtEntryDao
import com.thekeeperofpie.artistalleydatabase.art.data.ArtEntryDatabase
import com.thekeeperofpie.artistalleydatabase.art.data.ArtEntrySyncDao
import com.thekeeperofpie.artistalleydatabase.art.persistence.ArtExporter
import com.thekeeperofpie.artistalleydatabase.art.persistence.ArtImporter
import com.thekeeperofpie.artistalleydatabase.art.persistence.ArtSyncer
import com.thekeeperofpie.artistalleydatabase.browse.BrowseSelectionNavigator
import com.thekeeperofpie.artistalleydatabase.browse.BrowseTabViewModel
import com.thekeeperofpie.artistalleydatabase.data.DataConverter
import com.thekeeperofpie.artistalleydatabase.entry.EntryNavigator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object ArtEntryHiltModule {

    @Provides
    fun provideArtEntryDao(database: ArtEntryDatabase) = database.artEntryDao()

    @Provides
    fun provideArtEntryDetailsDao(database: ArtEntryDatabase) = database.artEntryDetailsDao()

    @Provides
    fun provideArtEntryBrowseDao(database: ArtEntryDatabase) = database.artEntryBrowseDao()

    @Provides
    fun provideArtEntrySyncDao(database: ArtEntryDatabase) = database.artEntrySyncDao()

    @Provides
    fun provideArtEntryAdvancedSearchDao(database: ArtEntryDatabase) =
        database.artEntryAdvancedSearchDao()

    @IntoSet
    @Provides
    fun provideArtExporter(
        application: Application,
        artEntryDao: ArtEntryDao,
        dataConverter: DataConverter,
        appJson: AppJson
    ): Exporter = ArtExporter(
        appContext = application,
        artEntryDao = artEntryDao,
        dataConverter = dataConverter,
        appJson = appJson
    )

    @IntoSet
    @Provides
    fun provideArtImporter(
        application: Application,
        artEntryDao: ArtEntryDao,
        moshi: Moshi,
    ): Importer = ArtImporter(
        appContext = application,
        artEntryDao = artEntryDao,
        moshi = moshi,
    )

    @IntoSet
    @Provides
    fun provideArtBrowseArtists(
        application: Application,
        artEntryBrowseDao: ArtEntryBrowseDao,
        artEntryNavigator: ArtEntryNavigator,
    ): BrowseTabViewModel = ArtBrowseTabArtists(application, artEntryBrowseDao, artEntryNavigator)

    @IntoSet
    @Provides
    fun provideArtBrowseCharacters(
        application: Application,
        artEntryBrowseDao: ArtEntryBrowseDao,
        artEntryNavigator: ArtEntryNavigator,
        appJson: AppJson,
        characterRepository: CharacterRepository,
    ): BrowseTabViewModel = ArtBrowseTabCharacters(
        application,
        artEntryBrowseDao,
        artEntryNavigator,
        appJson,
        characterRepository
    )

    @IntoSet
    @Provides
    fun provideArtBrowseSeries(
        application: Application,
        artEntryBrowseDao: ArtEntryBrowseDao,
        artEntryNavigator: ArtEntryNavigator,
        appJson: AppJson,
        mediaRepository: MediaRepository,
    ): BrowseTabViewModel = ArtBrowseTabSeries(
        application,
        artEntryBrowseDao,
        artEntryNavigator,
        appJson,
        mediaRepository
    )

    @IntoSet
    @Provides
    fun provideArtBrowseTags(
        application: Application,
        artEntryBrowseDao: ArtEntryBrowseDao,
        artEntryNavigator: ArtEntryNavigator,
    ): BrowseTabViewModel = ArtBrowseTabTags(application, artEntryBrowseDao, artEntryNavigator)

    @Provides
    fun provideArtEntryNavigator() = ArtEntryNavigator()

    @IntoSet
    @Provides
    fun provideArtSyncer(
        appJson: AppJson,
        artEntrySyncDao: ArtEntrySyncDao,
        characterRepository: CharacterRepository,
        characterEntryDao: CharacterEntryDao,
        mediaRepository: MediaRepository,
        mediaEntryDao: MediaEntryDao,
    ): DatabaseSyncer = ArtSyncer(
        appJson,
        artEntrySyncDao,
        characterRepository,
        characterEntryDao,
        mediaRepository,
        mediaEntryDao
    )

    @IntoSet
    @Provides
    fun bindArtEntryNavigatorAsBrowseSelectionNavigator(
        artEntryNavigator: ArtEntryNavigator
    ): BrowseSelectionNavigator =
        artEntryNavigator

    @IntoSet
    @Provides
    fun bindArtEntryNavigatorAsEntryNavigator(
        artEntryNavigator: ArtEntryNavigator
    ): EntryNavigator = artEntryNavigator
}