package org.mozilla.rocket.home.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.home.HomeViewModelFactory
import org.mozilla.rocket.home.pinsite.PinSiteManager
import org.mozilla.rocket.home.pinsite.SharedPreferencePinSiteDelegate
import org.mozilla.rocket.home.topsites.data.TopSitesRepo
import org.mozilla.rocket.home.topsites.domain.GetTopSitesUseCase
import javax.inject.Singleton

@Module
object HomeModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideHomeViewModelFactory(
        settings: Settings,
        getTopSitesUseCase: GetTopSitesUseCase
    ): HomeViewModelFactory = HomeViewModelFactory(settings, getTopSitesUseCase)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetTopSitesUseCase(topSitesRepo: TopSitesRepo): GetTopSitesUseCase = GetTopSitesUseCase(topSitesRepo)

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