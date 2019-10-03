package org.mozilla.rocket.content.travel.domain

import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.Result.Success
import org.mozilla.rocket.content.common.adapter.Runway
import org.mozilla.rocket.content.travel.data.City
import org.mozilla.rocket.content.travel.data.CityCategory
import org.mozilla.rocket.content.travel.data.RunwayItem
import org.mozilla.rocket.content.travel.data.TravelRepository
import org.mozilla.rocket.content.travel.ui.CityCategoryUiModel
import org.mozilla.rocket.content.travel.ui.CitySearchUiModel
import org.mozilla.rocket.content.travel.ui.CityUiModel
import org.mozilla.rocket.content.common.adapter.RunwayItem as RunwayItemUiModel

class GetExploreUseCase(private val travelRepository: TravelRepository) {

    private val data = ArrayList<DelegateAdapter.UiModel>()

    suspend operator fun invoke(): Result<List<DelegateAdapter.UiModel>> {
        data.clear()

        // addd search
        data.add(CitySearchUiModel())

        // add runway
        val runwayResult = travelRepository.getRunwayItems()
        if (runwayResult is Success) {
            data.add(
                    Runway(
                        "banner",
                        "banner",
                        0,
                        runwayResult.data.map {
                            toRunwayItemUiModel(it)
                        }
                    )
            )
        }

        // add city category
        val cityCategoryResult = travelRepository.getCityCategories()
        if (cityCategoryResult is Success) {
            data.addAll(
                    cityCategoryResult.data.map {
                        toCityCategoryUiModel(it)
                    }
            )
        }

        // TODO: handle error

        return Success(data)
    }

    private fun toRunwayItemUiModel(item: RunwayItem): RunwayItemUiModel =
            RunwayItemUiModel(
                item.source,
                item.imageUrl,
                item.linkUrl,
                "",
                item.id.toString()
            )

    private fun toCityCategoryUiModel(category: CityCategory): CityCategoryUiModel =
            CityCategoryUiModel(
                category.id,
                category.title,
                category.cityList.map {
                    toCityUiModel(it)
                }
            )

    private fun toCityUiModel(city: City): CityUiModel =
            CityUiModel(
                city.id,
                city.imageUrl,
                city.name
            )
}