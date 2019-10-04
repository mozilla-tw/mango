package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.Result.Success
import org.mozilla.rocket.content.Result.Error
import org.mozilla.rocket.content.travel.data.City
import org.mozilla.rocket.content.travel.data.TravelRepository
import org.mozilla.rocket.content.travel.ui.BucketListCityUiModel

class GetBucketListUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(): Result<List<DelegateAdapter.UiModel>> {
        val result = travelRepository.getBucketList()
        if (result is Success) {
            return Success(result.data.map {
                toBucketListCityUiModel(it)
            })
        }

        return Error(Exception("Fail to get bucket list"))
    }

    private fun toBucketListCityUiModel(city: City): BucketListCityUiModel =
            BucketListCityUiModel(
                city.id,
                city.imageUrl,
                city.name,
                true
            )
}