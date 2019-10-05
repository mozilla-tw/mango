package org.mozilla.rocket.content.travel.ui

import org.mozilla.rocket.adapter.DelegateAdapter

class CitySearchUiModel : DelegateAdapter.UiModel()

data class CityUiModel(
    val id: Int,
    val imageUrl: String,
    val name: String
) : DelegateAdapter.UiModel()

data class CityCategoryUiModel(
    val id: Int,
    val title: String,
    val cityList: List<CityUiModel>
) : DelegateAdapter.UiModel()

data class BucketListCityUiModel(
    val id: Int,
    val imageUrl: String,
    val name: String,
    val favorite: Boolean
) : DelegateAdapter.UiModel()

data class CitySearchResultUiModel(
    val id: Int,
    val name: CharSequence
)

data class SectionUiModel(
    val title: String,
    val linkUrl: String
) : DelegateAdapter.UiModel()

data class ExploreIgUiModel(
    val title: String,
    val linkUrl: String
) : DelegateAdapter.UiModel()

data class ExploreWikiUiModel(
    val imageUrl: String,
    val source: String,
    val introduction: String,
    val linkUrl: String
) : DelegateAdapter.UiModel()

data class VideoUiModel(
    val id: String,
    val imageUrl: String,
    val length: Int,
    val title: String,
    val author: String,
    val viewCount: Int,
    val date: String,
    val read: Boolean,
    val linkUrl: String
) : DelegateAdapter.UiModel()

data class HotelUiModel(
    val imageUrl: String,
    val source: String,
    val name: String,
    val distance: Float,
    val rating: Float,
    val freeWifi: Boolean,
    val price: Float,
    val currency: String,
    val freeCancellation: Boolean,
    val payAtProperty: Boolean,
    val linkUrl: String
) : DelegateAdapter.UiModel()