package org.mozilla.rocket.home.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.home.HomeViewModelFactory
import org.mozilla.rocket.home.pinsite.PinSiteManager
import org.mozilla.rocket.home.pinsite.SharedPreferencePinSiteDelegate
import org.mozilla.rocket.home.topsites.repository.TopSitesRepo
import javax.inject.Singleton

@Module
object HomeModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideHomeViewModelFactory(
        settings: Settings,
        topSitesRepo: TopSitesRepo
    ): HomeViewModelFactory = HomeViewModelFactory(settings, topSitesRepo)

    @JvmStatic
    @Singleton
    @Provides
    fun provideTopSitesRepo(
        appContext: Context,
        pinSiteManager: PinSiteManager
    ): TopSitesRepo = TopSitesRepo(appContext, pinSiteManager)

    @JvmStatic
    @Singleton
    @Provides
    fun providePinSiteManager(appContext: Context): PinSiteManager =
            PinSiteManager(SharedPreferencePinSiteDelegate(appContext))
}