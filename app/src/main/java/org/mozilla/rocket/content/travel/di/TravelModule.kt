package org.mozilla.rocket.content.travel.di

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mozilla.rocket.content.travel.data.TravelLocalDataSource
import org.mozilla.rocket.content.travel.data.TravelRemoteDataSource
import org.mozilla.rocket.content.travel.data.TravelRepository
import org.mozilla.rocket.content.travel.domain.GetBucketListUseCase
import org.mozilla.rocket.content.travel.domain.GetExploreUseCase
import org.mozilla.rocket.content.travel.ui.TravelBucketListViewModel
import org.mozilla.rocket.content.travel.ui.TravelExploreViewModel
import javax.inject.Singleton

@Module
object TravelModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideTravelRemoteDataSource(): TravelRemoteDataSource = TravelRemoteDataSource()

    @JvmStatic
    @Singleton
    @Provides
    fun provideTravelLocalDataSource(context: Context): TravelLocalDataSource = TravelLocalDataSource(context)

    @JvmStatic
    @Singleton
    @Provides
    fun provideTravelRepository(
        travelRemoteDataSource: TravelRemoteDataSource,
        travelLocalDataSource: TravelLocalDataSource
    ): TravelRepository = TravelRepository(travelRemoteDataSource, travelLocalDataSource)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetExploreUseCase(travelRepository: TravelRepository): GetExploreUseCase = GetExploreUseCase(travelRepository)

    @JvmStatic
    @Singleton
    @Provides
    fun provideGetBucketListUseCase(travelRepository: TravelRepository): GetBucketListUseCase = GetBucketListUseCase(travelRepository)

    @JvmStatic
    @Provides
    fun provideTravelExploreViewModel(getExploreUseCase: GetExploreUseCase): TravelExploreViewModel = TravelExploreViewModel(getExploreUseCase)

    @JvmStatic
    @Provides
    fun provideTravelBucketListViewModel(getBucketListUseCase: GetBucketListUseCase): TravelBucketListViewModel = TravelBucketListViewModel(getBucketListUseCase)
}