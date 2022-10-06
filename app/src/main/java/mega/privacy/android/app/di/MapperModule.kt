package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.mapper.DataMapper
import mega.privacy.android.app.data.mapper.PushMessageMapper
import mega.privacy.android.app.data.mapper.toData
import mega.privacy.android.app.data.mapper.toPushMessage
import mega.privacy.android.app.mediaplayer.mapper.RepeatModeMapper
import mega.privacy.android.app.mediaplayer.mapper.RepeatToggleModeMapper
import mega.privacy.android.app.mediaplayer.mapper.toRepeatModeMapper
import mega.privacy.android.app.mediaplayer.mapper.toRepeatToggleModeMapper
import mega.privacy.android.app.presentation.mapper.FavouriteMapper
import mega.privacy.android.app.presentation.mapper.toFavourite

/**
 * Module for providing mapper dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
class MapperModule {

    /**
     * Provide favourite mapper
     */
    @Provides
    fun provideFavouriteMapper(): FavouriteMapper = ::toFavourite

    /**
     * Provide data mapper
     */
    @Provides
    fun provideDataMapper(): DataMapper = ::toData

    /**
     * Provide push message mapper
     */
    @Provides
    fun providePushMessageMapper(): PushMessageMapper = ::toPushMessage

    /**
     * Provide the RepeatModeMapper
     */
    @Provides
    fun provideRepeatModeMapper(): RepeatModeMapper = ::toRepeatToggleModeMapper

    /**
     * Provide the RepeatToggleModeMapper
     */
    @Provides
    fun provideRepeatToggleModeMapper(): RepeatToggleModeMapper = ::toRepeatModeMapper
}