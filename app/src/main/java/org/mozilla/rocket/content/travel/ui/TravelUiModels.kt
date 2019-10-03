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