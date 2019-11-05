package org.mozilla.rocket.content.travel.data

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.common.data.ApiEntity
import org.mozilla.rocket.content.common.data.ApiItem

class TravelRepository(
    private val remoteDataSource: TravelRemoteDataSource,
    private val localDataSource: TravelLocalDataSource
) {

    suspend fun getExploreList(): Result<ApiEntity> {
        return localDataSource.getExploreList()
    }

    suspend fun getBucketList(): Result<List<BucketListCity>> {
        return localDataSource.getBucketList()
    }

    suspend fun searchCity(keyword: String): Result<List<ApiItem>> {
        return localDataSource.searchCity(keyword)
    }

    suspend fun getCityPriceItems(name: String): Result<List<PriceItem>> {
        return localDataSource.getCityPriceItems(name)
    }

    suspend fun getCityIg(name: String): Result<Ig> {
        return localDataSource.getCityIg(name)
    }

    suspend fun getCityWiki(name: String): Result<Wiki> {
        val resultExtract = remoteDataSource.getCityWikiExtract(name)
        val resultImage = remoteDataSource.getCityWikiImage(name)

        if (resultImage !is Result.Success || resultExtract !is Result.Success) {
            return Result.Error(Exception())
        }

        val wiki = Wiki(resultImage.data, resultExtract.data, WIKI_URL + name)

        return Result.Success(wiki)
    }

    suspend fun getCityVideos(name: String): Result<List<Video>> {
        return localDataSource.getCityVideos(name)
    }

    suspend fun getCityHotels(name: String): Result<List<Hotel>> {
        return localDataSource.getCityHotels(name)
    }

    suspend fun isInBucketList(id: String): Boolean {
        return localDataSource.isInBucketList(id)
    }

    suspend fun addToBucketList(city: BucketListCity) {
        localDataSource.addToBucketList(city)
    }

    suspend fun removeFromBucketList(id: String) {
        localDataSource.removeFromBucketList(id)
    }

    companion object {
        private const val WIKI_URL = "https://en.wikipedia.org/wiki/"
    }
}