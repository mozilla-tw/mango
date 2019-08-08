package org.mozilla.rocket.home.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.home.HomeViewModelFactory
import org.mozilla.rocket.home.topsites.repository.TopSitesRepo
import javax.inject.Singleton

@Module
object HomeModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideHomeViewModelFactory(settings: Settings): HomeViewModelFactory =
            HomeViewModelFactory(settings)

    @JvmStatic
    @Singleton
    @Provides
    fun provideTopSitesRepo(appContext: Context): TopSitesRepo = TopSitesRepo(appContext)
}