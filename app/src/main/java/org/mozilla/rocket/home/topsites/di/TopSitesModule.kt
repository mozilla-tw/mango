package org.mozilla.rocket.home.topsites.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.home.topsites.repository.TopSitesRepo
import javax.inject.Singleton

@Module
object TopSitesModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideTopSitesRepo(appContext: Context): TopSitesRepo = TopSitesRepo(appContext)
}